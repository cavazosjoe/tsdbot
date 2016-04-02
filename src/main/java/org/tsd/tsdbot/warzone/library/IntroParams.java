package org.tsd.tsdbot.warzone.library;

import java.util.Map;

public class IntroParams implements GeneratorParams {

    public static final String TOTAL_WINS = "totalWins";
    public static final String TOTAL_LOSSES = "totalLosses";
    public static final String TEAM_NAME = "teamName";

    public final int totalWins;
    public final int totalLosses;
    public final String teamName;

    public IntroParams(Map<String, Object> params) {
        this.totalWins = (int) params.get(TOTAL_WINS);
        this.totalLosses = (int) params.get(TOTAL_LOSSES);
        this.teamName = (String) params.get(TEAM_NAME);
    }

    public int totalGames() {
        return totalWins + totalLosses;
    }

    public double winPct() {
        return (double)totalWins / (double)(totalGames());
    }
}
