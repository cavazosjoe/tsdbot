package org.tsd.tsdbot.haloapi.model.stats;

import org.tsd.tsdbot.haloapi.model.Player;

import java.util.List;

public abstract class PlayerStat extends Stat {

    // The experience information for the player in this match.
    XpInfo XpInfo;

    // The number of times the player killed each opponent. If the player did not kill
    // an opponent, there will be no entry for that opponent.
    List<KilledDetail> KilledOpponentDetails;

    // The number of times the player was killed by each opponent. If the player was
    // not killed by an opponent, there will be no entry for that opponent.
    List<KilledDetail> KilledByOpponentDetails;

    // The game base variant specific stats for this match. Flexible stats are
    // available via the Metadata API.
    FlexibleStats FlexibleStats;

    // The set of rewards that the player got in this match. Rewards are available via
    // the Metadata API.
    List<RewardSet> RewardSets;

    // Details on any credits the player may have earned from playing this match.
    CreditsEarned CreditsEarned;

    // The player's progress towards meta commendations. Commendations that had no
    // progress earned this match will not be returned.
    List<MetaCommendationDelta> MetaCommendationDeltas;

    // The player's progress towards progressive commendations. Commendations that had
    // no progress earned this match will not be returned.
    List<ProgressiveDelta> ProgressiveCommendationDeltas;

    Player Player;

    // The ID of the team that the player was on when the match ended.
    int TeamId;

    // The player's team-agnostic ranking.
    int Rank;

    // Indicates whether the player was present in the match when it ended.
    boolean DNF;

    // The player's average lifetime.
    String AvgLifeTimeOfPlayer;

    // Internal use only. This will always be null.
    Object PreMatchRatings;

    // Internal use only. This will always be null.
    Object PostMatchRatings;

}
