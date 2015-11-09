package org.tsd.tsdbot.haloapi.model.stats;

// The experience information for the player in a match.
public class XpInfo {

    // The player's Spartan Rank before the match started.
    int PrevSpartanRank;

    // The player's Spartan Rank after the match ended.
    int SpartanRank;

    // The player's XP before the match started.
    int PrevTotalXP;

    // The player's XP after the match ended.
    int TotalXP;

    // The multiplier on the XP earned this match based on their Spartan Rank when
    // the match ended.
    double SpartanRankMatchXPScalar;

    // The portion of the XP the player earned this match that was based on how much
    // time was spent in-match.
    int PlayerTimePerformanceXPAward;

    // The XP awarded to the player based on how their team ranked when the match
    // concluded.
    int PerformanceXP;

    // The XP awarded to the player for their team-agnostic rank.
    int PlayerRankXPAward;

    // The amount of XP the player earned if they played a boost card for this match,
    // and the boost card criteria was met. This is a fixed amount of XP, not a
    // multiplier.
    int BoostAmount;

    public int getPrevSpartanRank() {
        return PrevSpartanRank;
    }

    public int getSpartanRank() {
        return SpartanRank;
    }

    public int getPrevTotalXP() {
        return PrevTotalXP;
    }

    public int getTotalXP() {
        return TotalXP;
    }

    public double getSpartanRankMatchXPScalar() {
        return SpartanRankMatchXPScalar;
    }

    public int getPlayerTimePerformanceXPAward() {
        return PlayerTimePerformanceXPAward;
    }

    public int getPerformanceXP() {
        return PerformanceXP;
    }

    public int getPlayerRankXPAward() {
        return PlayerRankXPAward;
    }

    public int getBoostAmount() {
        return BoostAmount;
    }
}
