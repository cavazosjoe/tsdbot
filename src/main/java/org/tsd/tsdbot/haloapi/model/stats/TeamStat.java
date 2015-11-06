package org.tsd.tsdbot.haloapi.model.stats;

import java.util.List;

public class TeamStat {

    // The ID for the team.
    int TeamId;

    // The team's score at the end of the match. The way the score is determined is
    // based off the game base variant being played:
    //   Breakout = number of rounds won,
    //   CTF = number of flag captures,
    //   Slayer = number of kills,
    //   Strongholds = number of points,
    //   Warzone = number of points.
    int Score;

    // The team's rank at the end of the match.
    int Rank;

    // The set of round stats for the team.
    List<RoundStat> RoundStats;

    public int getTeamId() {
        return TeamId;
    }

    public int getScore() {
        return Score;
    }

    public int getRank() {
        return Rank;
    }

    public List<RoundStat> getRoundStats() {
        return RoundStats;
    }
}
