package org.tsd.tsdbot.haloapi.model.stats.custom;

import org.tsd.tsdbot.haloapi.model.stats.FlexibleStats;
import org.tsd.tsdbot.haloapi.model.stats.Stat;

public class CustomGameBaseVariantStat extends Stat {

    // The game base variant specific stats. Flexible stats are available via
    // the Metadata API.
    FlexibleStats FlexibleStats;

    // The ID of the game base variant. Game base variants are available via
    // the Metadata API.
    String GameBaseVariantId;

    public FlexibleStats getFlexibleStats() {
        return FlexibleStats;
    }

    public String getGameBaseVariantId() {
        return GameBaseVariantId;
    }
}
