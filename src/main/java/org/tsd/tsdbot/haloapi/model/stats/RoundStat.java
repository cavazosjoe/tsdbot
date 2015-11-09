package org.tsd.tsdbot.haloapi.model.stats;

public class RoundStat {

    // The round number this entry pertains to.
    int RoundNumber;

    // The end rank for the team this round.
    int Rank;

    // The end score for the team this round.
    int Score;

    public int getRoundNumber() {
        return RoundNumber;
    }

    public int getRank() {
        return Rank;
    }

    public int getScore() {
        return Score;
    }
}
