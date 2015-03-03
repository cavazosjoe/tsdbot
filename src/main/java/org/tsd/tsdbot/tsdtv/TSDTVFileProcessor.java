package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.tsdtv.processor.*;
import org.tsd.tsdbot.util.TSDTVUtil;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 2/27/2015.
 */
@Singleton
public class TSDTVFileProcessor {

    private static final Logger log = LoggerFactory.getLogger(TSDTVFileProcessor.class);

    private static final Pattern durationLinePattern
            = Pattern.compile("\\s+Duration: (.*?), start: (.*?), bitrate: (\\d+) kb/s.*", Pattern.DOTALL);
    private static final Pattern durationPattern = Pattern.compile("(\\d+):(\\d+):(\\d+)\\.(\\d+)", Pattern.DOTALL);

    private static final Pattern streamLinePattern = Pattern.compile("\\s+Stream #\\d+:(\\d+)(\\(\\w+\\))?: (.*)", Pattern.DOTALL);
    private static final Pattern streamInfoPattern = Pattern.compile("(\\w+):\\s(\\w+).*", Pattern.DOTALL);

    @Inject
    private TSDBot bot;

    @Inject
    private ExecutorService executorService;

    @Inject @Named("serverUrl")
    private String serverUrl;

    @Inject @Named("ffmpegExec")
    private String ffmpegExec;

    @Inject @Named("tsdtvRaws")
    private File rawsDir;

    @Inject @Named("tsdtvLibrary")
    private File libraryDir;

    @Inject
    private Random random;

    private HashMap<String, AnalysisCollection> analyses = new HashMap<>();

    public AnalysisCollection getAnalysesForId(String id) {
        return analyses.get(id);
    }

    public void process(final String channel, String analysesId, final String output, final ProcessType type) throws TSDTVProcessingException, StreamDetectionException {

        log.info("Beginning show processing: analysisId={}, output={}, type={}", new Object[]{analysesId, output, type});

        final AnalysisCollection analysisCollection = analyses.get(analysesId);
        if(analysisCollection == null)
            throw new TSDTVProcessingException("Could not find analyses for id " + analysesId);
        if(analysisCollection.getAnalyses().size() == 0)
            throw new TSDTVProcessingException("Analysis collection for " + analysisCollection.getFolder() + " contains no analyses");

        File inputDir = new File(rawsDir + "/" + analysisCollection.getFolder());
        log.info("Using input directory {}", inputDir.getAbsolutePath());

        final File outputDir = new File(libraryDir.getAbsolutePath() + "/" + output);
        log.info("Using output directory {}", outputDir.getAbsolutePath());

        if(outputDir.exists())
            throw new TSDTVProcessingException("Output directory " + outputDir.getName() + " already exists");
        else
            outputDir.mkdir();

        final HashMap<Integer, String[]> manualStreamMapping = new HashMap<>();
        if(type.equals(ProcessType.manual)) try {

            File episodeFile = new File(inputDir.getAbsolutePath() + "/" + ".episodeMap");
            log.info("Using episodeFile {}", episodeFile.getAbsolutePath());

            if(!episodeFile.exists())
                throw new TSDTVProcessingException("Could not find episode stream mapping for " + analysisCollection.getFolder());

            FileInputStream episodeMapping = new FileInputStream(episodeFile);
            try(BufferedReader br = new BufferedReader(new InputStreamReader(episodeMapping))) {
                String line = null;
                int epNum = 0;
                while((line = br.readLine()) != null) {
                    manualStreamMapping.put(epNum, line.split(","));
                    epNum++;
                }
            } catch (IOException e) {
                log.error("Error reading episode mapping file", e);
                throw new TSDTVProcessingException("Error reading episode mapping file for " + analysisCollection.getFolder());
            }
        } catch (FileNotFoundException fnfe) {
            log.error("Error finding episode mapping file", fnfe);
            throw new TSDTVProcessingException("Error finding episode mapping file");
        }

        Runnable processingThread = new Runnable() {
            @Override
            public void run() {
                int epNum = 0;
                for(FileAnalysis fileAnalysis : analysisCollection.getAnalyses()) {
                    log.info("Processing fileAnalysis for {}", fileAnalysis.getFile().getAbsolutePath());
                    Stream[] tracks = null;
                    try {
                        switch (type) {
                            case dub:
                                tracks = detectDubStreams(fileAnalysis);
                                log.info("Dub streams: {}", tracks);
                                break;
                            case sub:
                                tracks = detectSubStreams(fileAnalysis);
                                log.info("Sub streams: {}", tracks);
                                break;
                            case manual: {
                                String[] tracksToUse = manualStreamMapping.get(epNum);
                                log.info("Tracks to use: {}", tracksToUse);
                                tracks = new Stream[tracksToUse.length];
                                for (int j = 0; j < tracksToUse.length; j++) {
                                    int n = Integer.parseInt(tracksToUse[j]);
                                    tracks[j] = fileAnalysis.getStreamsByInteger().get(n);
                                }
                                log.info("Manual streams: {}", tracks);
                                epNum++;
                            }
                        }
                    } catch (StreamDetectionException e) {
                        log.error("Error detecting streams for file {}", fileAnalysis.getFile().getName(), e);
                        bot.sendMessage(channel, "Error detecting streams for file " + fileAnalysis.getFile().getName());
                        return;
                    }

                    String outputFile = outputDir.getAbsolutePath() + "/" + fileAnalysis.getFile().getName();
                    log.info("Using outputLoc {}", outputFile);

                    LinkedList<String> ffmpegCmdParts = new LinkedList<>();
                    ffmpegCmdParts.add(ffmpegExec);
                    ffmpegCmdParts.add("-y");
                    ffmpegCmdParts.add("-i");
                    ffmpegCmdParts.add(fileAnalysis.getFile().getAbsolutePath());

                    for(Stream s : tracks) {
                        ffmpegCmdParts.add("-map");
                        ffmpegCmdParts.add("0:" + s.getStreamNumber());
                    }

                    ffmpegCmdParts.add("-c:v");
                    ffmpegCmdParts.add("copy");
                    ffmpegCmdParts.add("-c:a");
                    ffmpegCmdParts.add("copy");
                    ffmpegCmdParts.add(outputFile);

                    log.info("Using ffmpegCmdParts {}", ffmpegCmdParts);

                    ProcessBuilder pb = new ProcessBuilder(ffmpegCmdParts);
                    try {
                        Process p = pb.start();
                        p.waitFor();

                        InputStream out = p.getErrorStream();
                        InputStreamReader reader = new InputStreamReader(out);
                        BufferedReader br = new BufferedReader(reader);
                        String line;
                        while ((line = br.readLine()) != null) {
                            log.info(line);
                        }

                        log.info("Successfully processed file {}", fileAnalysis.getFile().getName());
                    } catch (Exception e) {
                        log.error("Error running ffmpeg command {}", pb.command(), e);
                        bot.sendMessage(channel, "Error processing file " + fileAnalysis.getFile().getName());
                        return;
                    }
                }

                StringBuilder successMsg = new StringBuilder();
                successMsg.append("Successfully processed ").append(analysisCollection.getAnalyses().size())
                        .append(" videos for ").append(output).append(". ")
                        .append(successFlavors[random.nextInt(successFlavors.length)]);
                bot.sendMessage(channel, successMsg.toString());
            }
        };

        executorService.submit(processingThread);
        bot.sendMessage(channel, "The request for processing has been sent to TSD Industries");

    }

    public void analyzeDirectory(String folderName, String channel) {
        analyzeDirectory(new File(rawsDir.getAbsolutePath() + "/" + folderName), channel);
    }

    public void analyzeDirectory(final File directory, final String channel) {

        Runnable analyzerThread = new Runnable() {
            @Override
            public void run() {
                AnalysisCollection collection = new AnalysisCollection(directory.getName());
                String err = "Error analyzing files: ";

                if(!directory.exists()) {
                    bot.sendMessage(channel, err + directory.getAbsolutePath() + " does not exist");
                    return;
                }

                if(!directory.isDirectory()) {
                    bot.sendMessage(channel, err + directory.getAbsolutePath() + " is not a directory");
                    return;
                }

                FileAnalysis analysis;
                List<File> files = Arrays.asList(directory.listFiles());
                Collections.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        try {
                            int result = Integer.compare(TSDTVUtil.getEpisodeNumberFromFilename(o1.getName()),
                                    TSDTVUtil.getEpisodeNumberFromFilename(o2.getName()));
                            if(result != 0)
                                return result;
                        } catch (Exception e) {}
                        return o1.compareTo(o2);
                    }
                });

                for(File f : files) try {
                    if(!f.getName().startsWith(".")) {
                        analysis = analyzeFile(f);
                        collection.addAnalysis(analysis);
                    }
                } catch (Exception e) {
                    log.error("Error analyzing file {}", f.getAbsolutePath(), e);
                    bot.sendMessage(channel, err + "Error analyzing file " + f.getAbsolutePath());
                    return;
                }

                analyses.put(collection.getId(), collection);
                collection.generateTextOutput();
                bot.sendMessage(channel, "Analysis complete: " + serverUrl + "/tsdtv/analyzer/" + collection.getId());
            }
        };

        executorService.submit(analyzerThread);
        bot.sendMessage(channel, "The request for analysis has been sent to TSD Labs");
    }

    public FileAnalysis analyzeFile(File f) throws IOException, InterruptedException, FileAnalysisException {
        Double bitrate = 0.0;
        long duration = 0;
        List<Stream> streams = new LinkedList<>();

        ProcessBuilder pb = new ProcessBuilder(ffmpegExec, "-i", f.getAbsolutePath());
        Process p = pb.start();
        p.waitFor();
        InputStream out = p.getErrorStream();
        InputStreamReader reader = new InputStreamReader(out);
        BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) try {

            // get the duration and bitrate from the "Duration:" line
            if(line.contains("Duration:")) {
                Matcher durationLineMatcher = durationLinePattern.matcher(line);
                while(durationLineMatcher.find()) {

                    // get duration
                    Matcher durationMatcher = durationPattern.matcher(durationLineMatcher.group(1));
                    while(durationMatcher.find()) {
                        duration += (Integer.parseInt(durationMatcher.group(1)) * 60 * 60 * 1000);
                        duration += (Integer.parseInt(durationMatcher.group(2)) * 60 * 1000);
                        duration += (Integer.parseInt(durationMatcher.group(3)) * 1000);
                    }

                    // get bitrate
                    bitrate = Double.parseDouble(durationLineMatcher.group(3));

                }
            }

            if(line.contains("Stream #")) {
                log.info("stream line: {}", line);
                Matcher streamLineMatcher = streamLinePattern.matcher(line);
                while(streamLineMatcher.find()) {

                    log.debug("group(1) = {}", streamLineMatcher.group(1));
                    log.debug("group(2) = {}", streamLineMatcher.group(2));
                    log.debug("group(3) = {}", streamLineMatcher.group(3));

                    StreamType type = null;
                    String codec = null;

                    String streamInfo = streamLineMatcher.group(3);
                    Matcher streamInfoMatcher = streamInfoPattern.matcher(streamInfo);
                    while(streamInfoMatcher.find()) {
                        type = StreamType.fromString(streamInfoMatcher.group(1));
                        codec = streamInfoMatcher.group(2);
                    }

                    if(type == null) {
                        log.warn("Could not determine stream type, skipping...");
                        continue;
                    }

                    int streamNumber = Integer.parseInt(streamLineMatcher.group(1));
                    boolean isDefault = streamInfo.contains("(default)");

                    // (eng)
                    String lang = streamLineMatcher.group(2);
                    lang = (StringUtils.isEmpty(lang)) ? null : lang.substring(1, lang.length()-1);

                    Stream stream = new Stream(type, streamNumber, isDefault, streamInfo, lang, codec);

                    streams.add(stream);
                }
            }
        } catch (Exception e) {
            log.error("Error processing information of file {}", f, e);
            throw new FileAnalysisException("Error processing information of file " + f.getAbsolutePath());
        }

        return new FileAnalysis(f, bitrate, duration, streams);
    }

    /**
     * For dubs we want:
     * 1. video
     * 2. english audio
     * @return dub streams
     */
    public static Stream[] detectDubStreams(FileAnalysis fileAnalysis) throws StreamDetectionException {
        Stream video = getVideoStream(fileAnalysis);
        Stream audio = getAudioStream(fileAnalysis, "eng");
        return new Stream[]{video, audio};
    }

    /**
     * For subs we want:
     * 1. video
     * 2. jpn audio
     * 3. eng subs
     * @return sub streams
     */
    public static Stream[] detectSubStreams(FileAnalysis fileAnalysis) throws StreamDetectionException {

        Stream video = getVideoStream(fileAnalysis);
        Stream audio = getAudioStream(fileAnalysis, "jpn");

        Stream subs = null;
        TreeSet<Stream> subStreams = fileAnalysis.getStreamsByType().get(StreamType.SUBTITLE);
        if(subStreams == null || subStreams.isEmpty())
            throw new StreamDetectionException("Could not detect any subtitle streams for " + fileAnalysis.getFile().getName());
        else if(subStreams.size() == 1) {
            if(subStreams.first().getLanguage() == null || "eng".equals(subStreams.first().getLanguage()))
                subs = subStreams.first();
        } else {
            for(Stream s : subStreams) {
                if("eng".equals(s.getLanguage())) {
                    subs = s;
                    break;
                }
            }
            if(subs == null) // couldn't find any explicitly english sub streams -- select the first one
                subs = subStreams.first();
        }

        if(subs == null)
            throw new StreamDetectionException("Could not detect any english subtitle streams for " + fileAnalysis.getFile().getName());

        return new Stream[]{video, audio, subs};

    }

    private static Stream getVideoStream(FileAnalysis fileAnalysis) throws StreamDetectionException {
        Stream video = null;
        TreeSet<Stream> videoStreams = fileAnalysis.getStreamsByType().get(StreamType.VIDEO);
        if(videoStreams == null || videoStreams.isEmpty())
            throw new StreamDetectionException("Could not detect any video streams for " + fileAnalysis.getFile().getName());
        else if(videoStreams.size() > 1)
            throw new StreamDetectionException("Found multiple video streams for " + fileAnalysis.getFile().getName());
        return videoStreams.first();
    }

    private static Stream getAudioStream(FileAnalysis fileAnalysis, String language) throws StreamDetectionException {
        Stream audio = null;
        TreeSet<Stream> audioStreams = fileAnalysis.getStreamsByType().get(StreamType.AUDIO);
        if(audioStreams == null || audioStreams.isEmpty())
            throw new StreamDetectionException("Could not detect any audio streams for " + fileAnalysis.getFile().getName());
        else if(audioStreams.size() == 1 || language == null)
            audio = audioStreams.first();
        else {
            for(Stream s : audioStreams) {
                if(language.equals(s.getLanguage())) {
                    audio = s;
                    break;
                }
            }
            if(audio == null)
                throw new StreamDetectionException("Found multiple audio streams but could not detect any ones for language " + language);
        }
        return audio;
    }

    public enum ProcessType {
        sub,
        dub,
        manual;

        public static ProcessType fromString(String s) {
            for(ProcessType type : values()) {
                if(type.toString().equals(s))
                    return type;
            }
            return null;
        }
    }

    private static final String[] successFlavors = new String[]{
            "Get dunked on you stupid retard.",
            "You're welcome.",
            "You're fucking welcome.",
            "They said it couldn't be done.",
            "Remember when kanbo said it couldn't be done, and then you did it?",
            "It's too bad TSD won't be around next year"
    };

}
