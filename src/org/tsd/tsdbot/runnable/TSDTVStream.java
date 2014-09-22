package org.tsd.tsdbot.runnable;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.functions.TSDTV;

import java.io.IOException;

/**
 * Created by Joe on 3/9/14.
 */
public class TSDTVStream implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(TSDTVStream.class);

    @Inject
    protected TSDTV tsdtv;

    @Inject @Named(value = "ffmpeg")
    private String ffmpegFormat;

    private String pathToMovie = null;

    @Deprecated
    public TSDTVStream() {}

    public void init(String pathToMovie) {
        ffmpegFormat = String.format(ffmpegFormat, pathToMovie);
    }

    public String getPathToMovie() {
        return pathToMovie;
    }

    public String getMovieName() {
        return pathToMovie.substring(pathToMovie.lastIndexOf("/")+1);
    }

    public boolean isInitialized() {
        return pathToMovie != null;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500); // wait for half a second
        } catch (InterruptedException e) {
            logger.error("Interrupted during stream delay");
            tsdtv.finishStream(false);
            return;
        }
        logger.info("[TSDTV] preparing movie " + pathToMovie);
        logger.info("[TSDTV] using command: {}", ffmpegFormat);
        ProcessBuilder pb = new ProcessBuilder(ffmpegFormat.split("\\s+"));
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

        tsdtv.finishStream(playNext);
    }

}
