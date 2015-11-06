package org.tsd.tsdbot.model.warzone;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by Joe on 10/31/2015.
 */
@DatabaseTable(tableName = "WarzoneGamePlayer")
public class WarzoneGamePlayer {

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = "warzoneGameId", canBeNull = false, uniqueCombo = true)
    private WarzoneGame game;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = "warzoneRegularId")
    private WarzoneRegular regular;

    @DatabaseField(canBeNull = false, uniqueCombo = true)
    private String gamertag;

    @DatabaseField(dataType = DataType.ENUM_STRING, canBeNull = false)
    private TeamColor teamColor;

    @DatabaseField(canBeNull = false)
    private int basesCaptured;

    @DatabaseField(canBeNull = false)
    private int bossTakedowns;

    @DatabaseField(canBeNull = false)
    private int spartanKills;

    @DatabaseField(canBeNull = false)
    private int npcKills;

    @DatabaseField(canBeNull = false)
    private int kda;

    @DatabaseField(canBeNull = false)
    private int powerWeaponKills;

    public WarzoneGame getGame() {
        return game;
    }

    public void setGame(WarzoneGame game) {
        this.game = game;
    }

    public WarzoneRegular getRegular() {
        return regular;
    }

    public void setRegular(WarzoneRegular regular) {
        this.regular = regular;
    }

    public String getGamertag() {
        return gamertag;
    }

    public void setGamertag(String gamertag) {
        this.gamertag = gamertag;
    }

    public TeamColor getTeamColor() {
        return teamColor;
    }

    public void setTeamColor(TeamColor teamColor) {
        this.teamColor = teamColor;
    }

    public int getBasesCaptured() {
        return basesCaptured;
    }

    public void setBasesCaptured(int basesCaptured) {
        this.basesCaptured = basesCaptured;
    }

    public int getBossTakedowns() {
        return bossTakedowns;
    }

    public void setBossTakedowns(int bossTakedowns) {
        this.bossTakedowns = bossTakedowns;
    }

    public int getSpartanKills() {
        return spartanKills;
    }

    public void setSpartanKills(int spartanKills) {
        this.spartanKills = spartanKills;
    }

    public int getNpcKills() {
        return npcKills;
    }

    public void setNpcKills(int npcKills) {
        this.npcKills = npcKills;
    }

    public int getKda() {
        return kda;
    }

    public void setKda(int kda) {
        this.kda = kda;
    }

    public int getPowerWeaponKills() {
        return powerWeaponKills;
    }

    public void setPowerWeaponKills(int powerWeaponKills) {
        this.powerWeaponKills = powerWeaponKills;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WarzoneGamePlayer that = (WarzoneGamePlayer) o;

        if (game != null ? !game.equals(that.game) : that.game != null) return false;
        return !(gamertag != null ? !gamertag.equals(that.gamertag) : that.gamertag != null);

    }

    @Override
    public int hashCode() {
        int result = game != null ? game.hashCode() : 0;
        result = 31 * result + (gamertag != null ? gamertag.hashCode() : 0);
        return result;
    }
}
