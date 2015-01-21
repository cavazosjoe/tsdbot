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

    @Inject @Named(value = "ffmpeg")
    private String ffmpegFormat;

    private TSDTVProgram movie = null;

    private File logFile = null;

    @Deprecated
    public TSDTVStream() {}

    public void init(String videoFilter, TSDTVProgram program) {
        this.movie = program;
        this.logFile = new File(properties.getProperty("tsdtv.log"));
        this.ffmpegFormat = String.format(ffmpegFormat, program.file.getAbsolutePath(), videoFilter);
    }

    public TSDTVProgram getMovie() {
        return movie;
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
        logger.info("[TSDTV] preparing movie {} using command {}", movie.file.getAbsolutePath(), ffmpegFormat);
        ProcessBuilder pb = new ProcessBuilder(ffmpegFormat.split("\\s+"));
        pb.redirectErrorStream(true);
        pb.redirectOutput(logFile);
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
