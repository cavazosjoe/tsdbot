package org.tsd.tsdbot.model.warzone;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import org.tsd.tsdbot.model.BasicEntity;

@DatabaseTable(tableName = "WarzoneGame")
public class WarzoneGame extends BasicEntity {

    @DatabaseField(unique = true)
    private String gameId;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = "warzoneNightId", canBeNull = false)
    private WarzoneNight night;

    @DatabaseField(canBeNull = false)
    private String gametype;

    @DatabaseField(canBeNull = false)
    private String map;

    @DatabaseField(canBeNull = false)
    private int duration; //seconds

    @DatabaseField(dataType = DataType.ENUM_STRING, canBeNull = false)
    private TeamColor teamColor;

    @DatabaseField(canBeNull = false)
    private int teamScore;

    @DatabaseField(canBeNull = false)
    private int enemyScore;

    @DatabaseField(canBeNull = false)
    private boolean win;

    @ForeignCollectionField(foreignFieldName = "game")
    private ForeignCollection<WarzoneGamePlayer> players;

    public ForeignCollection<WarzoneGamePlayer> getPlayers() {
        return players;
    }

    public void setPlayers(ForeignCollection<WarzoneGamePlayer> players) {
        this.players = players;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public WarzoneNight getNight() {
        return night;
    }

    public void setNight(WarzoneNight night) {
        this.night = night;
    }

    public String getGametype() {
        return gametype;
    }

    public void setGametype(String gametype) {
        this.gametype = gametype;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TeamColor getTeamColor() {
        return teamColor;
    }

    public void setTeamColor(TeamColor teamColor) {
        this.teamColor = teamColor;
    }

    public int getTeamScore() {
        return teamScore;
    }

    public void setTeamScore(int teamScore) {
        this.teamScore = teamScore;
    }

    public int getEnemyScore() {
        return enemyScore;
    }

    public void setEnemyScore(int enemyScore) {
        this.enemyScore = enemyScore;
    }

    public boolean isWin() {
        return win;
    }

    public void setWin(boolean win) {
        this.win = win;
    }
}
