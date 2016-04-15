package org.tsd.tsdbot.tsdfm;

import org.tsd.tsdbot.tsdfm.model.TSDFMSong;

import java.io.File;

public class TSDFMQueueItem {

    // this is the fully processed and filtered file, including intro speech, transitions, etc
    private final File file;
    private final TSDFMSong song;
    private final TSDFMBlock block;

    public TSDFMQueueItem(File file, TSDFMSong song) {
        this.file = file;
        this.song = song;
        this.block = null;
    }

    public TSDFMQueueItem(File file, TSDFMSong song, TSDFMBlock block) {
        this.file = file;
        this.song = song;
        this.block = block;
    }

    public File getFile() {
        return file;
    }

    public TSDFMSong getSong() {
        return song;
    }

    public TSDFMBlock getBlock() {
        return block;
    }
}
