package org.tsd.tsdbot.haloapi.model.stats;

public class RewardSet {

    // The ID of the reward.
    String RewardSet;

    // The source of the reward. Options are:
    //   None = 0,
    //   Meta Commendation = 1,
    //   Progress Commendation = 2,
    //   Spartan Rank = 3
    int RewardSourceType;

    // If the Reward Source is Spartan Rank, this value is set to the Spartan Rank
    // the player acquired that led to this reward being granted. Note: Unlike the
    // commendations fields in this structure, this is not the GUID to a Spartan
    // Rank content item. That's because the Spartan Rank content item itself does
    // not detail what specific Spartan Rank it pertains to - this information is
    // derived from the list of Spartan Ranks as a whole.
    int SpartanRankSource;

    // If the Reward Source is a Commendation, this is the ID of the level of the
    // commendation that earned the reward.
    String CommendationLevelId;

    // If the Reward Source is a Meta Commendation or Progress Commendation, this
    // is the ID of the Meta Commendation or Progress Commendation, respectively,
    // that earned the reward.
    String CommendationSource;

}
