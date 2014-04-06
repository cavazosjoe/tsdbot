package org.tsd.tsdbot.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.tsdtv.TSDTV;

import java.io.File;
import java.io.IOException;

/**
 * Created by Joe on 3/9/14.
 */
public class TSDTVStream implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(TSDTVStream.class);

    private String[] ffmpegParts;
    private String pathToMovie;

    public TSDTVStream(String[] ffmpegParts, String pathToMovie) {
        this.ffmpegParts = ffmpegParts;
        this.pathToMovie = pathToMovie;
    }

    public String getPathToMovie() {
        return pathToMovie;
    }

    @Override
    public void run() {
        logger.info("[TSDTV] preparing movie " + pathToMovie);
        ProcessBuilder pb = new ProcessBuilder(ffmpegParts);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        boolean playNext = true;
        try {
            Process p = pb.start();
            try {
                logger.info("TSDTV stream started, waiting...");
                p.waitFor();
                logger.info("TSDTV stream ended normally");
            } catch (InterruptedException e) {
                logger.info("TSDTV stream interrupted");
                playNext = false;
            } finally {
                p.destroy();
                logger.info("TSDTV stream destroyed");
            }
        } catch (IOException e) {
            logger.error("IOException", e);
        }

        TSDTV.getInstance().finishStream(playNext);
    }

}
