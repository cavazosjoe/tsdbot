package org.tsd.tsdbot.model.dboft;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by Joe on 2/7/2015.
 */
@DatabaseTable(tableName = "DBO_FIRETEAM_RSVP")
public class FireteamRSVP {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = "fireteamId", canBeNull = false)
    private Fireteam fireteam;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = "creatorId", canBeNull = false)
    private DboUser creator;

    @DatabaseField(canBeNull = false)
    private String gamertag;

    @DatabaseField
    private CharacterClass characterClass;

    @DatabaseField
    private Integer level;

    @DatabaseField
    private String comment;

    @DatabaseField
    private boolean tentative;

    @Deprecated
    public FireteamRSVP() {}

    public FireteamRSVP(Fireteam fireteam, DboUser creator, String gamertag) {
        this.fireteam = fireteam;
        this.creator = creator;
        this.gamertag = gamertag;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Fireteam getFireteam() {
        return fireteam;
    }

    public void setFireteam(Fireteam fireteam) {
        this.fireteam = fireteam;
    }

    public DboUser getCreator() {
        return creator;
    }

    public void setCreator(DboUser creator) {
        this.creator = creator;
    }

    public String getGamertag() {
        return gamertag;
    }

    public void setGamertag(String gamertag) {
        this.gamertag = gamertag;
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }

    public void setCharacterClass(CharacterClass characterClass) {
        this.characterClass = characterClass;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isTentative() {
        return tentative;
    }

    public void setTentative(boolean tentative) {
        this.tentative = tentative;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FireteamRSVP that = (FireteamRSVP) o;

        if (!creator.equals(that.creator)) return false;
        if (!fireteam.equals(that.fireteam)) return false;
        if (!gamertag.equals(that.gamertag)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fireteam.hashCode();
        result = 31 * result + creator.hashCode();
        result = 31 * result + gamertag.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RSVP " + fireteam.toString() + " " + gamertag;
    }
}
