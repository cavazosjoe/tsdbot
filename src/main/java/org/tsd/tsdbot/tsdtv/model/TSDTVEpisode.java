package org.tsd.tsdbot.tsdtv.model;

import org.tsd.tsdbot.util.TSDTVUtil;

import java.io.File;
import java.util.Comparator;

/**
 * Created by Joe on 2/1/2015.
 */
public class TSDTVEpisode extends Streamable implements Comparable<TSDTVEpisode> {

    private TSDTVShow show;
    private int episodeNumber = -1;

    public TSDTVEpisode(File file, TSDTVShow show) {
        super(file);
        this.show = show;
        try {
            this.episodeNumber = TSDTVUtil.getEpisodeNumberFromFilename(file.getName());
        } catch (Exception e) {
            this.episodeNumber = -1;
        }
    }

    public TSDTVShow getShow() {
        return show;
    }

    @Override
    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public String getRawName() {
        return file.getName();
    }

    public String getPrettyName() {
        return file.getName().replaceAll("_", " ");
    }

    @Override
    public boolean isBroadcastable() {
        return true;
    }

    @Override
    public String toString() {
        return show.getPrettyName() + ": " + getPrettyName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TSDTVEpisode that = (TSDTVEpisode) o;

        if (episodeNumber != that.episodeNumber) return false;
        if (!show.equals(that.show)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = show.hashCode();
        result = 31 * result + episodeNumber;
        return result;
    }

    @Override
    public int compareTo(TSDTVEpisode o) {
        int i = Integer.compare(episodeNumber, o.getEpisodeNumber());
        return (i != 0) ? i : getRawName().compareToIgnoreCase(o.getRawName());
    }
}
