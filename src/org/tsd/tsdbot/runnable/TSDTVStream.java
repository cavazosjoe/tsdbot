package org.tsd.tsdbot.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDTV;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by Joe on 3/9/14.
 */
public class TSDTVStream implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(TSDTVStream.class);

    private TSDTV controller;

    private String scriptDir;
    private String movie;

    public TSDTVStream(TSDTV controller, String scriptDir, String movieToPlay) {
        this.controller = controller;
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
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (IOException ioe) {
            logger.error("IOException", ioe);
        } catch (InterruptedException e) {
            logger.info("TSDTV stream interrupted");
        }
        controller.finishStream();
    }
}
