package org.tsd.tsdbot.tsdtv.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.tsdtv.TSDTVConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 2/1/2015.
 */
public abstract class Streamable {

    private final static Logger logger = LoggerFactory.getLogger(Streamable.class);

    private static final Pattern durationPattern = Pattern.compile("(\\d+):(\\d+):(\\d+)\\.(\\d+)");

    protected File file;

    public Streamable(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public abstract boolean isBroadcastable();
    public abstract int getEpisodeNumber();

    public long getDuration(String ffmpegExec) {
        String durationString = getMetadata(ffmpegExec).get(TSDTVConstants.METADATA_DURATION_FIELD);
        logger.info("Parsing duration {} for file {}", durationString, file.getAbsolutePath());
        Matcher m = durationPattern.matcher(durationString);
        long duration = 0;
        while(m.find()) {
            duration += (Integer.parseInt(m.group(1)) * 60 * 60 * 1000);
            duration += (Integer.parseInt(m.group(2)) * 60 * 1000);
            duration += (Integer.parseInt(m.group(3)) * 1000);
        }
        return duration;
    }

    public HashMap<String, String> getMetadata(String ffmpegExec) {

        logger.info("Retrieving metadata for {}", file.getAbsolutePath());
        HashMap<String, String> metadata = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegExec, "-i", file.getAbsolutePath());
            Process p = pb.start();
            p.waitFor();
            InputStream out = p.getErrorStream();
            InputStreamReader reader = new InputStreamReader(out);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while( (line = br.readLine()) != null ) {
                logger.info(line);
                if(line.contains("Metadata")) {
                    while( (line = br.readLine()) != null && (!line.contains("Duration")) ) { // stop on duration
                        String[] parts = line.split(":",2);
                        if(parts.length == 2) {
                            logger.info("Adding line to metadata: {} -> {}", parts[0].trim(), parts[1].trim());
                            metadata.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
                if(line != null && line.contains("Duration")) {
                    // now get the duration
                    // Duration: 00:00:00.0, start=0000blahblah
                    logger.info("Raw duration line: {}", line);
                    String duration = line.substring(line.indexOf(":") + 1, line.indexOf(","));
                    logger.info("Adding parsed duration to metadata: {}", duration);
                    metadata.put(TSDTVConstants.METADATA_DURATION_FIELD, duration);
                    break;
                }
            }

        } catch (Exception e) {
            logger.error("Error getting video metadata", e);
        }

        return metadata;

    }
}
