package org.tsd.tsdbot.tsdtv.model;

import java.io.File;

/**
 * Created by Joe on 2/1/2015.
 */
public class TSDTVFiller extends Streamable {

    private FillerType fillerType;

    public TSDTVFiller(File file, FillerType fillerType) {
        super(file);
        this.fillerType = fillerType;
    }

    public FillerType getFillerType() {
        return fillerType;
    }

    @Override
    public boolean isBroadcastable() {
        return false;
    }

    @Override
    public int getEpisodeNumber() {
        return -1;
    }

    @Override
    public String toString() {
        return fillerType.getDisplayString() + ": " + file.getName().replaceAll("_", " ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TSDTVFiller that = (TSDTVFiller) o;

        if (fillerType != that.fillerType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return fillerType.hashCode();
    }
}
