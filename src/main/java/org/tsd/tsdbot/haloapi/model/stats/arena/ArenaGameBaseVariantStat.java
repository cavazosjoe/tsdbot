package org.tsd.tsdbot.haloapi.model.stats.arena;

import org.tsd.tsdbot.haloapi.model.stats.Stat;

public class ArenaGameBaseVariantStat extends Stat {

    // The game base variant specific stats. Flexible stats are available via
    // the Metadata API.
    org.tsd.tsdbot.haloapi.model.stats.FlexibleStats FlexibleStats;

    // The ID of the game base variant. Game base variants are available via
    // the Metadata API.
    String GameBaseVariantId;
}
