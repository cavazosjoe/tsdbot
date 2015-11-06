package org.tsd.tsdbot.haloapi.model.stats.arena;

import org.tsd.tsdbot.haloapi.model.stats.Stat;
import org.tsd.tsdbot.haloapi.model.stats.TopGameBaseVariant;

import java.util.List;

public class ArenaStats extends Stat {

    // List of arena stats by playlist.
    List<ArenaPlaylistStat> ArenaPlaylistStats;

    // The highest obtained CSR by the player in arena. If the player hasn't
    // finished measurement matches yet for any playlist, this value is null.
    Csr HighestCsrAttained;

    // List of arena stats by GameBaseVariant
    List<ArenaGameBaseVariantStat> ArenaGameBaseVariantStats;

    // A list of up to 3 game base variants with the highest win rate by the user.
    // If there is a tie, the one with more completions is higher. If there's still
    // a tie, the GUIDs are sorted and selected.
    List<TopGameBaseVariant> TopGameBaseVariants;

    // The ID for the playlist that pertains to the highest obtained CSR field. If
    // the CSR is null, so is this field.
    String HighestCsrPlaylistId;

    public List<ArenaPlaylistStat> getArenaPlaylistStats() {
        return ArenaPlaylistStats;
    }

    public Csr getHighestCsrAttained() {
        return HighestCsrAttained;
    }

    public List<ArenaGameBaseVariantStat> getArenaGameBaseVariantStats() {
        return ArenaGameBaseVariantStats;
    }

    public List<TopGameBaseVariant> getTopGameBaseVariants() {
        return TopGameBaseVariants;
    }

    public String getHighestCsrPlaylistId() {
        return HighestCsrPlaylistId;
    }
}
