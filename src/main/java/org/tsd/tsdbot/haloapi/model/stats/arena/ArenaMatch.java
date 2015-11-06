package org.tsd.tsdbot.haloapi.model.stats.arena;

import org.tsd.tsdbot.haloapi.model.stats.MultiplayerMatch;

import java.util.List;

public class ArenaMatch extends MultiplayerMatch {

    List<ArenaPlayerStat> PlayerStats;

    public List<ArenaPlayerStat> getPlayerStats() {
        return PlayerStats;
    }
}
