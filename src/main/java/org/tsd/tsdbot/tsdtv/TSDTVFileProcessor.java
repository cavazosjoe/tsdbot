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
import java.nio.file.NotDirectoryException;
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

    private HashMap<String, AnalysisCollection> analyses = new HashMap<>();

    public AnalysisCollection getAnalysesForId(String id) {
        return analyses.get(id);
    }

    public void process(String rawsLocation, String destinationName) {


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
                    analysis = analyzeFile(f);
                    collection.addAnalysis(analysis);
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

                    log.info("group(1) = {}", streamLineMatcher.group(1));
                    log.info("group(2) = {}", streamLineMatcher.group(2));
                    log.info("group(3) = {}", streamLineMatcher.group(3));

                    StreamType type = null;
                    String codec = null;

                    String streamInfo = streamLineMatcher.group(3);
                    Matcher streamInfoMatcher = streamInfoPattern.matcher(streamInfo);
                    while(streamInfoMatcher.find()) {
                        type = StreamType.fromString(streamInfoMatcher.group(1));
                        codec = streamInfoMatcher.group(2);
                    }

                    if(type == null) {
                        log.info("Could not determine stream type, skipping...");
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

}
