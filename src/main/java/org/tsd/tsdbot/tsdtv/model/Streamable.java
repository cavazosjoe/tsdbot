package org.tsd.tsdbot.tsdtv.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class Streamable {

    private final static Logger log = LoggerFactory.getLogger(Streamable.class);



    protected File file;

    public Streamable(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public abstract boolean isBroadcastable();
    public abstract int getEpisodeNumber();


}
