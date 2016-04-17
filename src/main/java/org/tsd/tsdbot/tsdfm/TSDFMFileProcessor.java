package org.tsd.tsdbot.tsdfm;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.tsdfm.model.TSDFMSong;
import org.tsd.tsdbot.util.FfmpegUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TSDFMFileProcessor {

    private static final Logger log = LoggerFactory.getLogger(TSDFMFileProcessor.class);

    private final VoiceRssClient voiceRssClient;
    private final FfmpegUtils ffmpegUtils;
    private final String ffmpegExec;
    private final String tsdfmLog;

    @Inject
    public TSDFMFileProcessor(VoiceRssClient voiceRssClient,
                              FfmpegUtils ffmpegUtils,
                              @Named("tsdfmLog") String tsdfmLog,
                              @Named("ffmpegExec") String ffmpegExec) {
        this.voiceRssClient = voiceRssClient;
        this.ffmpegExec = ffmpegExec;
        this.ffmpegUtils = ffmpegUtils;
        this.tsdfmLog = tsdfmLog;
    }

    public File addIntroToSong(String introText, TSDFMSong song) throws Exception {

        log.info("Adding intro text to song, text=\"{}\", song={}", introText, song.getMusicFile().getAbsolutePath());

        Path introFilePath = null;
        try {
            byte[] introSpeechBytes = voiceRssClient.getSpeech(introText);
            if(introSpeechBytes.length < 1000) {
                // probably an error
                log.error("PROBABLE ERROR getting voiceRss data: {}", new String(introSpeechBytes));
            } else {
                log.info("Retrieved intro speech file, size = {} kB", introSpeechBytes.length/1000);
            }
            introFilePath = Files.createTempFile("introFile", ".mp3");
            introFilePath = Files.write(introFilePath, introSpeechBytes);

            String complexFilter =
                    String.format(
                            "\"[0:a] afade=t=in:ss=0:d=%s [music];[1:a] volume=6 [speech];[music][speech] amix [out]\"",
                            (ffmpegUtils.getDuration(introFilePath.toFile())/1000)+2
                    );

            Path tempFilePath = Files.createTempFile("outputFile", ".mp3");

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegExec,
                    "-y",
                    "-i", String.format("\"%s\"", song.getMusicFile().getAbsolutePath()),
                    "-i", String.format("\"%s\"", introFilePath.toAbsolutePath().toString()),
                    "-filter_complex", complexFilter,
                    "-map", "\"[out]\"",
                    tempFilePath.toAbsolutePath().toString()
            );

            pb.redirectErrorStream(true);
            pb.redirectOutput(new File(tsdfmLog));
            try {
                Process p = pb.start();
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    log.info("TSDFM intro processor interrupted", e);
                } finally {
                    p.destroy();
                }
            } catch (IOException e) {
                log.error("IOException", e);
            }

            return tempFilePath.toFile();

        } finally {
            if(introFilePath != null) {
                Files.delete(introFilePath);
            }
        }
    }


}
