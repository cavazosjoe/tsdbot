package org.tsd.tsdbot.haloapi.model.stats.warzone;

import org.tsd.tsdbot.haloapi.model.stats.MultiplayerMatch;

import java.util.List;

public class WarzoneMatch extends MultiplayerMatch {

    List<WarzonePlayerStat> PlayerStats;

    public List<WarzonePlayerStat> getPlayerStats() {
        return PlayerStats;
    }
}
