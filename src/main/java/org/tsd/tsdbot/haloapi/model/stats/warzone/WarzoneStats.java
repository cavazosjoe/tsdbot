package org.tsd.tsdbot.haloapi.model.stats.warzone;

import org.tsd.tsdbot.haloapi.model.stats.Stat;

import java.util.List;

public class WarzoneStats extends Stat {

    // The total number of "pies" (in-game currency) the player has earned.
    int TotalPiesEarned;

    // List of scenario stats by map and game base variant id.
    List<ScenarioStat> ScenarioStats;

}
