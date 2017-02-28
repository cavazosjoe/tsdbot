package org.tsd.tsdbot.model.warzone;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.tsd.tsdbot.model.BasicEntity;

@DatabaseTable(tableName = "WarzoneGamePlayer")
public class WarzoneGamePlayer extends BasicEntity {

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
}
