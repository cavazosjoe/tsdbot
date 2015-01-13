package org.tsd.tsdbot.tsdtv;

/**
 * Created by Joe on 1/12/2015.
 */
public class ShowInfo {
    public String name;
    public int previousEpisode;
    public int nextEpisode;

    public ShowInfo(String name, int previousEpisode, int nextEpisode) {
        this.name = name;
        this.previousEpisode = previousEpisode;
        this.nextEpisode = nextEpisode;
    }
}
