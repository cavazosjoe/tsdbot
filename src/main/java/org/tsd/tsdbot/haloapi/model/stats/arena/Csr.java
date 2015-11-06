package org.tsd.tsdbot.haloapi.model.stats.arena;

public class Csr {

    // The CSR tier.
    int Tier;

    // The Designation of the CSR. Options are:
    //   1 through 5: Normal designations
    //   6 and 7: Semi-pro and Pro respectively
    int DesignationId;

    // The CSR value. Zero for normal designations.
    int Csr;

    // The percentage of progress towards the next CSR tier.
    int PercentToNextTier;

    // If the CSR is Semi-pro or Pro, the player's leaderboard ranking.
    int Rank;

}
