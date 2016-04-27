package org.tsd.tsdbot.tsdfm;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.config.TSDBotConfiguration;

import java.io.File;
import java.io.IOException;

public class TSDFMStream extends Thread {

    private static final Logger log = LoggerFactory.getLogger(TSDFMStream.class);

    private final TSDFM tsdfm;
    private final String ffmpegExec;
    private final String tsdfmOut;
    private final File logFile;

    private TSDFMQueueItem musicItem;
    private String ffmpegCommand;

    @Inject
    public TSDFMStream(TSDFM tsdfm,
                       TSDBotConfiguration configuration,
                       @Named(value = "ffmpegExec") String ffmpegExec,
                       @Named(value = "tsdfmOut") String tsdfmOut) {
        this.tsdfm = tsdfm;
        this.ffmpegExec = ffmpegExec;
        this.tsdfmOut = tsdfmOut;
        this.logFile = new File(configuration.tsdfm.logFile);
    }

    public void init(TSDFMQueueItem musicItem) {
        this.musicItem = musicItem;
        this.ffmpegCommand = String.format("%s -re -i \"%s\" %s",
                ffmpegExec, musicItem.getFile().getAbsolutePath(), tsdfmOut);
    }

    public TSDFMQueueItem getMusicItem() {
        return musicItem;
    }

    @Override
    public void run() {
        log.debug("[TSDFM] preparing music {} using command {}", musicItem, ffmpegCommand);
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegExec, "-re", "-i", musicItem.getFile().getAbsolutePath(), tsdfmOut
        );
        pb.redirectErrorStream(true);
        pb.redirectOutput(logFile);
        boolean error = false;
        try {
            Process p = pb.start();
            try {
                log.debug("TSDFM stream started, playing...");
                int exit = p.waitFor();
                if(exit == 0) {
                    log.debug("TSDFM stream ended normally");
                } else {
                    log.error("TSDFM stream ended with ERROR, code " + exit);
                    error = true;
                }
            } catch (InterruptedException e) {
                log.debug("TSDFM stream interrupted");
            } finally {
                p.destroy();
                log.debug("TSDFM stream destroyed");
            }
        } catch (IOException e) {
            log.error("IOException", e);
            error = true;
        }

        try {
            tsdfm.handleStreamEnd(error);
        } catch (Exception e) {
            log.error("Error playing next item");
        }
    }
}
