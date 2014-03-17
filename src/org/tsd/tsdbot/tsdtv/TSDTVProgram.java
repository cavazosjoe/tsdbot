package org.tsd.tsdbot.tsdtv;

/**
 * Created by Joe on 3/17/14.
 */
public class TSDTVProgram {

    public String filePath;
    public String show;
    public int episodeNum;

    public TSDTVProgram(String filePath, String show, int episodeNum) {
        this.filePath = filePath;
        this.show = show;
        this.episodeNum = episodeNum;
    }

    public TSDTVProgram(String filePath) {
        this.filePath = filePath;
        this.show = null;
        this.episodeNum = -1;
    }
}
