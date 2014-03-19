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

    public String getMovie() {
        return movie;
    }

    @Override
    public void run() {
        logger.info("[TSDTV] preparing movie " + movie);
        ProcessBuilder pb = new ProcessBuilder("./tsdtv.sh", movie);
        pb.directory(new File(scriptDir));
        boolean playNext = true;
        try {
            Process p = pb.start();
            try {
                while(isRunning(p)) {
                    Thread.sleep(5000);
                    if(Thread.interrupted()) {
                        logger.info("TSDTV detected thread interrupt");
                        throw new InterruptedException();
                    }
                }
            } catch (InterruptedException e) {
                logger.info("TSDTV stream interrupted");
                p.destroy();
                logger.info("TSDTV stream destroyed");
                playNext = false;
            }
        } catch (IOException e) {
            logger.error("IOException", e);
        }

        TSDTV.getInstance().finishStream(playNext);
    }

    private boolean isRunning(Process p) {
        try {
            p.exitValue();
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
