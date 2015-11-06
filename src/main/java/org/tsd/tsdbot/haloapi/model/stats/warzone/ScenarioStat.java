package org.tsd.tsdbot.haloapi.model.stats.warzone;

import org.tsd.tsdbot.haloapi.model.stats.FlexibleStats;
import org.tsd.tsdbot.haloapi.model.stats.Stat;

public class ScenarioStat extends Stat {

    // The total number of "pies" (in-game currency) the player has earned in
    // the scenario.
    int TotalPiesEarned;

    // The game base variant specific stats. Flexible stats are available via
    // the Metadata API.
    FlexibleStats FlexibleStats;

    // The map global ID that this warzone scenario pertains to. Found in
    // metadata
    String MapId;

    // The ID of the game base variant. Game base variants are available via
    // the Metadata API.
    String GameBaseVariantId;

}
