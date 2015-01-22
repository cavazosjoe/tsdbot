package org.tsd.tsdbot.tsdtv;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Joe on 3/17/14.
 */
public class TSDTVProgram {

    public File file;
    public String show;
    public TSDTV.TSDTVBlock block;
    public int episodeNum;
    public Date startTime;
    public Date endTime;

    public TSDTVProgram(File file, String show, TSDTV.TSDTVBlock block, int episodeNum, Date startTime, Date endTime) {
        this.file = file;
        this.show = show;
        this.episodeNum = episodeNum;
        this.startTime = startTime;
        this.endTime = endTime;
        this.block = block;
    }

    public TSDTVProgram(File file, String show, TSDTV.TSDTVBlock block, Date startTime, Date endTime) {
        this.file = file;
        this.show = show;
        this.episodeNum = -1;
        this.startTime = startTime;
        this.endTime = endTime;
        this.block = block;
    }

    public File getFile() {
        return file;
    }

    public String getShow() {
        return show;
    }

    public int getEpisodeNum() {
        return episodeNum;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public String toPrettyString() {
        return show + ": " + file.getName();
    }

    @Override
    public String toString() {
        return "TSDTVProgram{" +
                "file=" + file +
                ", show='" + show + '\'' +
                ", episodeNum=" + episodeNum +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TSDTVProgram program = (TSDTVProgram) o;

        if (episodeNum != program.episodeNum) return false;
        if (!file.equals(program.file)) return false;
        if (!show.equals(program.show)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = file.hashCode();
        result = 31 * result + show.hashCode();
        result = 31 * result + episodeNum;
        return result;
    }
}
