package org.tsd.tsdbot.markov;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.util.MarkovUtil;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

@Singleton
public class MarkovFileManager {

    private static final Logger log = LoggerFactory.getLogger(MarkovFileManager.class);

    private static final String wordDelimiter = ",";

    private final File baseDir;

    @Inject
    public MarkovFileManager(File baseDir) {
        this.baseDir = baseDir;
    }

    public List<File> allFiles() {
        return Arrays.asList(baseDir.listFiles());
    }

    protected File getFile(String name) throws IOException {
        return createFileIfNotExists(name);
    }

    protected File createFileIfNotExists(String name) throws IOException {
        File f = new File(baseDir, name);
        if(!f.exists()) {
            log.info("Markov file {} does not exist, creating...", name);
            f.createNewFile();
        }
        log.info("Found markov file: {} -> {}", name, f);
        return f;
    }

    public void addToFile(String filename, MarkovKey key, String... values) throws IOException {
        File markovFile = getFile(filename);

        // during loop, record each line so the file can be re-compiled at the end
        File temp = Files.createTempFile(RandomStringUtils.randomAlphabetic(20), ".txt").toFile();

        log.info("Adding to markov file: key=\"{}\", values={}", key, ArrayUtils.toString(values));
        boolean foundKey = false;
        try(
                BufferedReader reader = new BufferedReader(new FileReader(markovFile));
                BufferedWriter tempWriter = new BufferedWriter(new FileWriter(temp))
        ) {
            String line;
            while((line = reader.readLine()) != null) {
                log.trace("Evaluating markov line: {}", line);
                if(line.startsWith(key.toString())) {
                    log.info("Line starts with key: {}", key);
                    foundKey = true;
                    String[] words = line.substring(key.toString().length()).split(wordDelimiter);
                    log.info("Parsed words: {}", Arrays.toString(words));
                    for(String value : values) {
                        value = MarkovUtil.sanitize(value);
                        log.info("Sanitized word: {}", value);
                        words = ArrayUtils.add(words, value);
                    }
                    line = key.toString()+StringUtils.join(words, wordDelimiter);
                    log.info("Adding revised line: {}", line);
                }
                tempWriter.write(line);
                tempWriter.newLine();
            }

            if(!foundKey) {
                String[] words = new String[values.length];
                for(int i=0 ; i < values.length ; i++) {
                    String value = MarkovUtil.sanitize(values[i]);
                    log.info("Sanitized word: {}", value);
                    words[i] = value;
                }
                line = key.toString()+StringUtils.join(words, wordDelimiter);
                log.info("Did not find key in markov file, adding line: {}", line);
                tempWriter.write(line);
            }
        }

        FileUtils.copyFile(temp, markovFile);
        if(!temp.delete()) {
            log.warn("Temporary file not deleted: {}", temp);
        }
    }

    public String[] getWordsForKey(String filename, MarkovKey key) throws IOException {
        File markovFile = getFile(filename);
        log.info("Reading from markov file: key=\"{}\"", key);

        try(BufferedReader reader = new BufferedReader(new FileReader(markovFile))) {
            String line;
            while((line = reader.readLine()) != null) {
                log.trace("Evaluating markov line: {}", line);
                if(line.startsWith(key.toString())) {
                    log.info("Line starts with key: {}", key);
                    String wordsRaw = line.substring(key.toString().length());
                    log.info("Parsed words: {}", wordsRaw);
                    return StringUtils.isNotBlank(wordsRaw) ? wordsRaw.split(wordDelimiter) : null;
                }
            }
        }

        log.info("Failed to find line for key: {}", key);
        return null;
    }
}
