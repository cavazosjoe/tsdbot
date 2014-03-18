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

    private String scriptDir;
    private String movie;

    public TSDTVStream(String scriptDir, String movieToPlay) {
        this.scriptDir = scriptDir;
        this.movie = movieToPlay;
    }

    @Override
    public void run() {
        logger.info("[TSDTV] preparing movie " + movie);
        ProcessBuilder pb = new ProcessBuilder("./tsdtv.sh", movie);
        pb.directory(new File(scriptDir));
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        boolean playNext = true;
        synchronized (TSDTV.getInstance()) {
            try {
                Process p = pb.start();
                p.waitFor();
            } catch (IOException ioe) {
                logger.error("IOException", ioe);
            } catch (InterruptedException e) {
                logger.info("TSDTV stream interrupted");
                playNext = false;
            }
        }
        TSDTV.getInstance().finishStream(playNext);
    }
}
