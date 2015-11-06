package org.tsd.tsdbot.haloapi.model.stats.arena;

import org.tsd.tsdbot.haloapi.model.stats.PlayerStat;

public class ArenaPlayerStat extends PlayerStat {

    // The Competitive Skill Ranking (CSR) of the player before the match started. If
    // the player is still in measurement matches, this field is null. If the player
    // finished the last measurement match this match, this field is still null.
    Csr PreviousCsr;

    // The Competitive Skill Ranking (CSR) of the player after the match ended. If the
    // player is still in measurement matches, this field is null.
    Csr CurrentCsr;

    // The player's measurement matches left. If this field is greater than zero, then
    // the player will not have a CSR yet. If the player finished the match, this match
    // is included in this count.
    int MeasurementMatchesLeft;

    public Csr getPreviousCsr() {
        return PreviousCsr;
    }

    public Csr getCurrentCsr() {
        return CurrentCsr;
    }

    public int getMeasurementMatchesLeft() {
        return MeasurementMatchesLeft;
    }
}
