package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by Joe on 3/9/14.
 */
public class TSDTVStream extends Thread {

    private static Logger logger = LoggerFactory.getLogger(TSDTVStream.class);

    @Inject
    protected TSDTV tsdtv;

    @Inject
    protected Properties properties;

    @Inject @Named(value = "ffmpegExec")
    private String ffmpegExec;

    @Inject @Named(value = "ffmpegArgs")
    private String ffmpegArgs;

    @Inject @Named(value = "ffmpegOut")
    private String ffmpegOut;

    private String ffmpegCommand;

    private TSDTVQueueItem movie = null;

    private File logFile = null;

    private boolean playNext = true;

    @Deprecated
    public TSDTVStream() {}

    public void init(String videoFilter, TSDTVQueueItem program) {
        this.movie = program;
        this.logFile = new File(properties.getProperty("tsdtv.log"));
        this.ffmpegCommand = ffmpegExec + " " + ffmpegArgs + " " + ffmpegOut;
        this.ffmpegCommand = String.format(ffmpegCommand, movie.video.getFile().getAbsolutePath(), videoFilter);
    }

    public TSDTVQueueItem getMovie() {
        return movie;
    }

    public void kill(boolean playNext) {
        this.playNext = playNext;
        this.interrupt();
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
        logger.info("[TSDTV] preparing movie {} using command {}", movie.video.getFile().getAbsolutePath(), ffmpegCommand);
        ProcessBuilder pb = new ProcessBuilder(ffmpegCommand.split("\\s+"));
        pb.redirectErrorStream(true);
        pb.redirectOutput(logFile);
        try {
            Process p = pb.start();
            try {
                logger.info("TSDTV stream started, waiting...");
                p.waitFor();
                logger.info("TSDTV stream ended normally");
            } catch (InterruptedException e) {
                logger.info("TSDTV stream interrupted");
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
