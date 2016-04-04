package org.tsd.tsdbot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.util.FuzzyLogic;

import java.io.*;
import java.util.*;

@Singleton
public class FilenameLibrary implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(FilenameLibrary.class);

    private static final int MAX_SUBMISSIONS_PER_PERSON = 5;
    private static final int MAX_TOTAL_SUBMISSIONS = 20;

    private static final long MAX_FILESIZE = 1024 * 1024 * 5; // 5 megabytes
    private static final String[] VALID_FILE_TYPES = {"jpg", "jpeg", "bmp", "webm", "flv", "png", ".mp3", "gif", "gifv"};

    private final LinkedList<FilenameSubmission> submissionQueue = new LinkedList<>();
    private final File filenameDirectory;
    private final HttpClient httpClient;
    private final Random random;
    private final String serverUrl;

    @Inject
    public FilenameLibrary(@Named("filenameLibrary") File filenameDirectory,
                           HttpClient httpClient,
                           Random random,
                           @Named("serverUrl") String serverUrl) {
        this.filenameDirectory = filenameDirectory;
        this.httpClient = httpClient;
        this.random = random;
        this.serverUrl = serverUrl;
    }

    public String addFilename(String name, String path) throws IOException, FilenameValidationException {
        name = correctNameIfNecessary(name, path);
        byte[] bytes = validateAndDownload(name, path);
        writeToFile(name, bytes);
        return serverUrl + "/filenames/" + name;
    }

    public String submitFilename(String name, String path, String submitterNick, String submitterIdent) throws FilenameValidationException {

        if(submissionQueue.size() >= MAX_TOTAL_SUBMISSIONS) {
            throw new FilenameValidationException("there are currently too many submissions pending");
        }

        int pendingForThisUser = 0;
        for(FilenameSubmission submission : submissionQueue) {
            if(submission.getSubmitterIdent().equals(submitterIdent))
                pendingForThisUser++;
        }

        if(pendingForThisUser >= MAX_SUBMISSIONS_PER_PERSON) {
            throw new FilenameValidationException(String.format(
                    "you currently have %s submissions pending. Ask an op to review them", pendingForThisUser));
        }

        name = correctNameIfNecessary(name, path);
        byte[] bytes = validateAndDownload(name, path);
        FilenameSubmission submission = new FilenameSubmission(
                name, path, bytes, submitterNick, submitterIdent
        );
        submissionQueue.addLast(submission);
        return submission.getId();
    }

    public String approve(String id) throws IOException {
        Iterator<FilenameSubmission> iterator = submissionQueue.iterator();
        while(iterator.hasNext()) {
            FilenameSubmission submission = iterator.next();
            if (submission.getId().equals(id)) try {
                File f = writeToFile(submission.getName(), submission.getBytes());
                return serverUrl + "/filenames/" + f.getName();
            } finally {
                iterator.remove();
            }
        }
        throw new RuntimeException("Could not find submission with ID " + id);
    }

    public void deny(String id) throws FilenameRetrievalException {
        Iterator<FilenameSubmission> iterator = submissionQueue.iterator();
        while(iterator.hasNext()) {
            FilenameSubmission submission = iterator.next();
            if(submission.getId().equals(id)) {
                iterator.remove();
                return;
            }
        }
        throw new FilenameRetrievalException("could not find pending submission with id " + id);
    }

    public List<FilenameSubmission> listSubmissions() {
        return Collections.unmodifiableList(submissionQueue);
    }

    public String get(String contains) throws FilenameRetrievalException {
        TreeSet<File> filenames = readFiles();
        if(filenames.isEmpty())
            throw new FilenameRetrievalException("no filenames available");

        if(StringUtils.isBlank(contains)) {
            // return random
            int randomIdx = random.nextInt(filenames.size());
            int i = 0;
            for (File file : filenames) {
                if (i == randomIdx) {
                    return serverUrl + "/filenames/" + file.getName();
                }
                i++;
            }
            throw new FilenameRetrievalException("no filenames available");
        } else {
            LinkedList<File> matchedFiles = FuzzyLogic.fuzzySubset(contains, filenames, new FuzzyLogic.FuzzyVisitor<File>() {
                @Override
                public String visit(File o1) {
                    return o1.getName();
                }
            });

            if(matchedFiles.size() == 0)
                throw new FilenameRetrievalException("found no filenames matching \""+contains+"\"");

            File toReturn = matchedFiles.get(random.nextInt(matchedFiles.size()));
            return serverUrl + "/filenames/" + toReturn.getName();
        }
    }

    public byte[] getFile(String name) throws Exception {
        TreeSet<File> filenames = readFiles();
        for(File f : filenames) {
            if(f.getName().equals(name))
                return IOUtils.toByteArray(new FileInputStream(f));
        }
        throw new FileNotFoundException();
    }

    public TreeSet<File> readFiles() {
        TreeSet<File> filenames = new TreeSet<>(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        filenames.addAll(Arrays.asList(filenameDirectory.listFiles()));
        return filenames;
    }

    File writeToFile(String name, byte[] bytes) throws IOException {
        File targetFile = new File(filenameDirectory, name);
        FileUtils.writeByteArrayToFile(targetFile, bytes);
        return targetFile;
    }

    String correctNameIfNecessary(String name, String path) throws FilenameValidationException {
        String extension = parseExtensionFromName(name);
        if(extension == null) {
            // the submitter didn't include an extension in their filename, help them out
            // by inferring it from the URL
            extension = parseExtensionFromName(path);
            if(extension == null) {
                throw new FilenameValidationException("could not parse file type from URL");
            }
        } else {
            // the submitter did include an extension in their filename
            // check to make sure it matches the URL
            String pathExt = parseExtensionFromName(path);
            if(pathExt == null) {
                throw new FilenameValidationException("could not parse file type from URL");
            }
            if(!extension.equalsIgnoreCase(pathExt)) {
                throw new FilenameValidationException("URL must match filetype of name: " + extension);
            }
        }
        if(!name.endsWith("."+extension)) {
            name += ("."+extension);
        }
        return name;
    }

    byte[] validateAndDownload(String name, String path) throws FilenameValidationException {

        log.info("Validating filename submission: name={} path={}", name, path);

        if(StringUtils.isBlank(name))
            throw new FilenameValidationException("name cannot be null");
        if(StringUtils.isBlank(path))
            throw new FilenameValidationException("URL cannot be null");

        if(!UrlValidator.getInstance().isValid(path))
            throw new FilenameValidationException("not a valid URL");


        String extension = parseExtensionFromName(name);
        if(!Arrays.asList(VALID_FILE_TYPES).contains(extension.toLowerCase())) {
            throw new FilenameValidationException("invalid file type. Must be one of " + Arrays.toString(VALID_FILE_TYPES));
        }

        for(File f : readFiles()) {
            if(f.getName().equalsIgnoreCase(name))
                throw new FilenameValidationException("detected an existing filename named " + name);
        }

        for(FilenameSubmission submission : submissionQueue) {
            if(submission.getName().equalsIgnoreCase(name))
                throw new FilenameValidationException("detected a pending filename named " + name);
        }

        try {
            HttpGet get = new HttpGet(path);
            HttpResponse response = httpClient.execute(get);
            int status = response.getStatusLine().getStatusCode();
            if(status/100 != 2)
                throw new FilenameValidationException("Error downloading file: status code " + status);
            byte[] bytes = EntityUtils.toByteArray(response.getEntity());
            if(bytes.length > MAX_FILESIZE) {
                long maxKb = MAX_FILESIZE / (1024);
                long kb = bytes.length / (1024);
                throw new FilenameValidationException(String.format("filesize of %s KB is above the max of %s KB", kb, maxKb));
            }
            return bytes;
        } catch (IOException e) {
            log.error("Error downloading file " + path, e);
            throw new FilenameValidationException("Error downloading file");
        }
    }

    String parseExtensionFromName(String name) {
        if(name.contains(".")) {
            String[] parts = name.split("\\.");
            return parts[parts.length-1];
        } else {
            return null;
        }
    }

    public class FilenameValidationException extends Exception {
        public FilenameValidationException(String message) {
            super(message);
        }
    }

    public class FilenameRetrievalException extends Exception {
        public FilenameRetrievalException(String message) {
            super(message);
        }
    }

    public class FilenameSubmission {
        private String id;
        private String name;
        private String url;
        private byte[] bytes;
        private String submitterNick;
        private String submitterIdent;
        private Date submittedOn;

        public FilenameSubmission(String name, String url, byte[] bytes, String submitterNick, String submitterIdent) {
            this.name = name;
            this.url = url;
            this.bytes = bytes;
            this.submitterNick = submitterNick;
            this.submitterIdent = submitterIdent;
            this.submittedOn = new Date();
            this.id = RandomStringUtils.randomAlphabetic(10);
        }

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        public String getName() {
            return name;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getSubmitterNick() {
            return submitterNick;
        }

        public String getSubmitterIdent() {
            return submitterIdent;
        }

        public Date getSubmittedOn() {
            return submittedOn;
        }
    }

}
