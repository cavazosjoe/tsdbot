package org.tsd.tsdbot.haloapi.model.stats.custom;

import org.tsd.tsdbot.haloapi.model.stats.Stat;
import org.tsd.tsdbot.haloapi.model.stats.TopGameBaseVariant;

import java.util.List;

public class CustomStats extends Stat {

    // List of custom stats by CustomGameBaseVariant.
    List<CustomGameBaseVariantStat> CustomGameBaseVariantStats;

    // A list of up to 3 top game base variants played by the user Top means
    // Wins/Completed matches. If there is a tie, the one with more completions is
    // higher. If there's still a tie, the GUIDs are sorted and selected
    List<TopGameBaseVariant> TopGameBaseVariants;

    public List<CustomGameBaseVariantStat> getCustomGameBaseVariantStats() {
        return CustomGameBaseVariantStats;
    }

    public List<TopGameBaseVariant> getTopGameBaseVariants() {
        return TopGameBaseVariants;
    }
}
