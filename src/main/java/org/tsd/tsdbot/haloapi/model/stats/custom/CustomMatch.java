package org.tsd.tsdbot.haloapi.model.stats.custom;

import org.tsd.tsdbot.haloapi.model.stats.MultiplayerMatch;
import org.tsd.tsdbot.haloapi.model.stats.PlayerStat;

import java.util.List;

public class CustomMatch extends MultiplayerMatch {

    // A list of stats for each player who was present in the match.
    List<PlayerStat> PlayerStats;

    public List<PlayerStat> getPlayerStats() {
        return PlayerStats;
    }
}
