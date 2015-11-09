package org.tsd.tsdbot.warzone;

import com.google.inject.Inject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import org.tsd.tsdbot.database.JdbcConnectionProvider;
import org.tsd.tsdbot.haloapi.HaloApiClient;
import org.tsd.tsdbot.haloapi.MetadataCache;
import org.tsd.tsdbot.haloapi.model.stats.PlayerStat;
import org.tsd.tsdbot.haloapi.model.stats.TeamStat;
import org.tsd.tsdbot.haloapi.model.stats.warzone.WarzoneMatch;
import org.tsd.tsdbot.haloapi.model.stats.warzone.WarzonePlayerStat;
import org.tsd.tsdbot.model.warzone.WarzoneRegular;

import java.util.*;

public class RecapGenerator {

    private final HaloApiClient apiClient;
    private final MetadataCache metadataCache;
    private final Random random;
    private final Library library;
    private final JdbcConnectionProvider connectionProvider;

    @Inject
    public RecapGenerator(HaloApiClient apiClient,
                          MetadataCache metadataCache,
                          Random random,
                          JdbcConnectionProvider connectionProvider,
                          Library library) {
        this.apiClient = apiClient;
        this.metadataCache = metadataCache;
        this.random = random;
        this.connectionProvider = connectionProvider;
        this.library = library;
    }

    public void generateRecap(List<String> gameIds) throws Exception {

        Map<String, WarzoneRegular> regulars = getAllRegulars();
        List<WarzoneMatch> matches = fetchMatches(gameIds);

        StringBuilder content = new StringBuilder();

    }

    Map<String, WarzoneRegular> getAllRegulars() throws Exception {
        Dao<WarzoneRegular, String> dao = DaoManager.createDao(connectionProvider.get(), WarzoneRegular.class);
        Map<String, WarzoneRegular> map = new HashMap<>();
        for(WarzoneRegular reg : dao.queryForAll()) {
            map.put(reg.getGamertag(), reg);
        }
        return map;
    }

    List<WarzoneMatch> fetchMatches(List<String> gameIds) throws Exception {
        List<WarzoneMatch> matches = new LinkedList<>();
        for(String id : gameIds) {
            matches.add(apiClient.getWarzoneMatch(id));
        }
        return matches;
    }

    String generateOpeningParagraph(List<WarzoneMatch> matches, Map<String, WarzoneRegular> regularsInDb) {

        int[] winLossTie = new int[3];
        HashMap<String, WarzoneRegularStats> regularsInGames = new HashMap<>();

        for(WarzoneMatch match : matches) {

            for(WarzonePlayerStat playerStat : match.getPlayerStats()) {
                String gt = playerStat.getPlayer().getGamertag();
                if(regularsInDb.containsKey(gt) && !regularsInGames.containsKey(gt)) {
                    regularsInGames.put(gt, new WarzoneRegularStats(regularsInDb.get(gt)));
                }
                regularsInGames.get(gt).process(playerStat);
            }

            TeamStat teamStats = identifyTeamByRegulars(match, regularsInDb.keySet());
            if(teamStats.getRank() == 1)
                winLossTie[0]++;
            else
                winLossTie[1]++;

        }

        return null;

    }

    String generateTeamName(Map<String, WarzoneRegularStats> regularsInGames) {
        int gamesCount = 0;
        String personInMostGames = null;
        for(String gt : regularsInGames.keySet()) {
            if(regularsInGames.get(gt).gameCount() > gamesCount) {
                personInMostGames = gt;
                gamesCount = regularsInGames.get(gt).gameCount();
            }
        }
        return String.format(library.teamNames.pop(), personInMostGames);
    }

    TeamStat identifyTeamByRegulars(WarzoneMatch match, Set<String> regulars) {
        int[] regularsOnTeam = new int[match.getTeamStats().size()];
        for(PlayerStat playerStat : match.getPlayerStats()) {
            if(regulars.contains(playerStat.getPlayer().getGamertag()))
                regularsOnTeam[playerStat.getTeamId()] += 1;
        }

        int mostRegulars = 0;
        int mostRegularsIdx = -1;

        for(int i=0 ; i < regularsOnTeam.length ; i++) {
            if(regularsOnTeam[i] > mostRegulars) {
                mostRegulars = regularsOnTeam[i];
                mostRegularsIdx = i;
            }
        }

        for(TeamStat teamStat : match.getTeamStats()) {
            if(teamStat.getTeamId() == mostRegularsIdx)
                return teamStat;
        }

        return null;
    }

    // wraps a regular and keeps track of running stats (total kills, etc)
    class WarzoneRegularStats {

        final WarzoneRegular regular;

        int totalKills;
        int totalDeaths;
        int spartanRankOld;
        int spartanRankNew;
        double avgRank;

        List<WarzonePlayerStat> games = new LinkedList<>();

        public WarzoneRegularStats(WarzoneRegular regular) {
            this.regular = regular;
        }

        public void process(WarzonePlayerStat warzonePlayerStat) {
            games.add(warzonePlayerStat);
            totalKills += warzonePlayerStat.getTotalSpartanKills();
            totalDeaths += warzonePlayerStat.getTotalDeaths();
            if(games.size() == 1) {
                spartanRankOld = warzonePlayerStat.getXpInfo().getPrevSpartanRank();
            }
            spartanRankNew = warzonePlayerStat.getXpInfo().getSpartanRank();
            avgRank = (avgRank + warzonePlayerStat.getRank()) / games.size();
        }

        public int gameCount() {
            return games.size();
        }

        public int kdSpread() {
            return totalKills - totalDeaths;
        }

        public int rankDelta() {
            return spartanRankNew - spartanRankOld;
        }
    }

}
