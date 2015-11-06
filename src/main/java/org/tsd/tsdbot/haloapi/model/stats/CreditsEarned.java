package org.tsd.tsdbot.haloapi.model.stats;

public class CreditsEarned {

    // Indicates how the credits result was arrived at. Options are:
    //   Credits Disabled In Playlist = 0,
    //   Player Did Not Finish = 1,
    //   Credits Earned = 2
    // Credits Disabled In Playlist: TotalCreditsEarned is zero because this playlist
    // has credits disabled.
    // Player Did Not Finish: Credits are enabled in this playlist, but
    // TotalCreditsEarned is zero because the player did not finish the match.
    // Credits Earned: Credits are enabled in this playlist and the player completed
    // the match, so the credits formula was successfully evaluated. The fields below
    // provide the client with the values used in the formula. Note: That if we used
    // one or more default values, we still return "NormalResult". The fields below
    // will confirm the actual values used.
    int Result;

    // The total number of credits the player earned from playing this match.
    int TotalCreditsEarned;

    // The scalar applied to the credits earned based on the player's Spartan Rank.
    double SpartanRankModifier;

    // The portion of credits earned due to the player's team-agnostic rank in the
    // match.
    int PlayerRankAmount;

    // The portion of credits earned due to the time the player played in the match.
    double TimePlayedAmount;

    // The portion of credits earned due to the boost card the user applied
    int BoostAmount;
}
