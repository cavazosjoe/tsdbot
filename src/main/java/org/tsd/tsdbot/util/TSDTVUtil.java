package org.tsd.tsdbot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.tsdtv.FileAnalysisException;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TSDTVUtil {

    private static final Logger log = LoggerFactory.getLogger(TSDTVUtil.class);

    public static int getEpisodeNumberFromFilename(String fileName) throws Exception {
        Pattern episodeNumberPattern = Pattern.compile("^(\\d+).*",Pattern.DOTALL);
        Matcher epNumMatcher = episodeNumberPattern.matcher(fileName);
        while(epNumMatcher.find()) {
            return Integer.parseInt(epNumMatcher.group(1));
        }
        throw new Exception("Could not parse episode number from String " + fileName);
    }

    public static File getRandomFileFromDirectory(Random random, File dir) {
        if(dir.exists()) {
            File[] files = dir.listFiles();
            if(files == null || files.length == 0) {
                return null;
            }
            return files[random.nextInt(files.length)];
        }
        return null;
    }

    public static double getMaxVolume(String ffmpegExec, File file) throws FileAnalysisException {

        log.info("Retrieving max volume for {}", file.getAbsolutePath());

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegExec,
                    "-i", file.getAbsolutePath(),
                    "-af", "volumedetect",
                    "-f", "null",
                    "/dev/null");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();

            try(InputStream out = p.getInputStream();
                InputStreamReader reader = new InputStreamReader(out);
                BufferedReader br = new BufferedReader(reader)) {

                String line;
                Double maxVolume = null;
                while ( maxVolume == null && (line = br.readLine()) != null ) {
                    log.debug(line);
                    if (line.contains("max_volume")) {
                        String volString = line.substring(line.indexOf(":")+1, line.indexOf("dB"));
                        maxVolume = Double.parseDouble(volString.trim());
                    }
                }

                if(maxVolume == null) {
                    throw new Exception("Could not find max_volume");
                }

                return maxVolume;
            }

        } catch (Exception e) {
            log.error("Error parsing max_volume for {}", file.getAbsolutePath(), e);
            throw new FileAnalysisException("Could not parse max_volume for " + file.getAbsolutePath());
        }
    }
}
