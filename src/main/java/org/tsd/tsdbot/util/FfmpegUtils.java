package org.tsd.tsdbot.util;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.tsdtv.TSDTVConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FfmpegUtils {

    private static final Logger log = LoggerFactory.getLogger(FfmpegUtils.class);
    private static final Pattern durationPattern = Pattern.compile("(\\d+):(\\d+):(\\d+)\\.(\\d+)");

    private final String ffprobeExec;

    @Inject
    public FfmpegUtils(@Named("ffprobeExec") String ffprobeExec) {
        this.ffprobeExec = ffprobeExec;
    }

    public long getDuration(File file) {
        String durationString = getMetadata(file).get(TSDTVConstants.METADATA_DURATION_FIELD);
        log.debug("Parsing duration {} for file {}", durationString, file.getAbsolutePath());
        Matcher m = durationPattern.matcher(durationString);
        long duration = 0;
        while(m.find()) {
            duration += (Integer.parseInt(m.group(1)) * 60 * 60 * 1000);
            duration += (Integer.parseInt(m.group(2)) * 60 * 1000);
            duration += (Integer.parseInt(m.group(3)) * 1000);
        }
        return duration;
    }

    public HashMap<String, String> getMetadata(File file) {

        log.debug("Retrieving metadata for {}", file.getAbsolutePath());
        HashMap<String, String> metadata = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(ffprobeExec, "-i", file.getAbsolutePath());
            Process p = pb.start();
            p.waitFor();
            InputStream out = p.getErrorStream();
            InputStreamReader reader = new InputStreamReader(out);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while( (line = br.readLine()) != null ) {
                log.debug(line);
                if(line.contains("Metadata")) {
                    while( (line = br.readLine()) != null && (!line.contains("Duration")) ) { // stop on duration
                        String[] parts = line.split(":",2);
                        if(parts.length == 2) {
                            log.debug("Adding line to metadata: {} -> {}", parts[0].trim(), parts[1].trim());
                            metadata.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
                if(line != null && line.contains("Duration")) {
                    // now get the duration
                    // Duration: 00:00:00.0, start=0000blahblah
                    log.debug("Raw duration line: {}", line);
                    String duration = line.substring(line.indexOf(":") + 1, line.indexOf(","));
                    log.debug("Adding parsed duration to metadata: {}", duration);
                    metadata.put(TSDTVConstants.METADATA_DURATION_FIELD, duration);
                    break;
                }
            }

        } catch (Exception e) {
            log.error("Error getting video metadata", e);
        }

        return metadata;
    }
}
