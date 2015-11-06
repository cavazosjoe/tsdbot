package org.tsd.tsdbot.haloapi.model.stats.arena;

import org.tsd.tsdbot.haloapi.model.stats.Stat;

public class ArenaPlaylistStat extends Stat {

    // The playlist ID. Playlists are available via the Metadata API.
    String PlaylistId;

    // The player's measurement matches left. If this field is greater than
    // zero, then the player will not have a CSR yet.
    int MeasurementMatchesLeft;

    // The highest Competitive Skill Ranking (CSR) achieved by the player. This
    // is included because a player's CSR can drop based on performance.
    Csr HighestCsr;

    // The current Competitive Skill Ranking (CSR) of the player.
    Csr Csr;

}
