package org.tsd.tsdbot.tsdtv;

import org.tsd.tsdbot.tsdtv.model.Streamable;

import java.io.File;
import java.util.Date;

/**
 * Created by Joe on 3/17/14.
 */
public class TSDTVQueueItem {

    public Streamable video;
    public TSDTV.TSDTVBlock block;
    public boolean scheduled;
    public Date startTime;
    public Date endTime;

    public TSDTVQueueItem(Streamable video, TSDTV.TSDTVBlock block, boolean scheduled, Date startTime, String ffmpegExec) {
        this.video = video;
        this.block = block;
        this.scheduled = scheduled;
        this.startTime = startTime;
        long duration = video.getDuration(ffmpegExec);
        this.endTime = new Date(startTime.getTime() + duration);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TSDTVQueueItem that = (TSDTVQueueItem) o;

        if (scheduled != that.scheduled) return false;
        if (block != null ? !block.equals(that.block) : that.block != null) return false;
        if (!endTime.equals(that.endTime)) return false;
        if (!startTime.equals(that.startTime)) return false;
        if (!video.equals(that.video)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = video.hashCode();
        result = 31 * result + (block != null ? block.hashCode() : 0);
        result = 31 * result + (scheduled ? 1 : 0);
        result = 31 * result + startTime.hashCode();
        result = 31 * result + endTime.hashCode();
        return result;
    }
}
