package org.tsd.tsdbot.tsdtv;

import org.tsd.tsdbot.tsdtv.model.Streamable;

import java.util.Date;

public class TSDTVQueueItem {

    public Streamable video;
    public TSDTV.TSDTVBlock block;
    public boolean scheduled;
    public Date startTime;
    public Date endTime;
    public TSDTVUser owner;   // the person who started this stream, and the person who can end it
                                    // web page -> InetAddress ;; chat -> nick

    public TSDTVQueueItem(Streamable video, TSDTV.TSDTVBlock block,
                          boolean scheduled, Date startTime, long duration, TSDTVUser owner) {
        this.video = video;
        this.block = block;
        this.scheduled = scheduled;
        this.startTime = startTime;
        this.endTime = new Date(startTime.getTime() + duration);
        this.owner = owner;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TSDTVQueueItem: ");
        if(block != null)
            sb.append(block.name).append(" -- ");
        sb.append(video.getFile().getAbsolutePath());
        return sb.toString();
    }
}
