package org.tsd.tsdbot.haloapi.model.stats.warzone;

import org.tsd.tsdbot.haloapi.model.stats.PlayerStat;

public class WarzonePlayerStat extends PlayerStat {

    // The maximum level the player achieved in the match.
    int WarzoneLevel;

    // The total number of "pies" (in-game currency) the player earned in the match.
    int TotalPiesEarned;

    public int getWarzoneLevel() {
        return WarzoneLevel;
    }

    public int getTotalPiesEarned() {
        return TotalPiesEarned;
    }
}
