package org.tsd.tsdbot.model.dbo.fireteam;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.tsd.tsdbot.model.BasicEntity;
import org.tsd.tsdbot.model.dbo.DboUser;

@DatabaseTable(tableName = "DBO_FIRETEAM_RSVP")
public class FireteamRSVP extends BasicEntity {

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

        return new EqualsBuilder()
                .append(fireteam, that.fireteam)
                .append(creator, that.creator)
                .append(gamertag, that.gamertag)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(fireteam)
                .append(creator)
                .append(gamertag)
                .toHashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(gamertag);
        if(characterClass != null) {
            sb.append(", ").append(characterClass.getDisplayString());
        }
        if(level != null) {
            sb.append(", Level ").append(level);
        }
        if(tentative) {
            sb.append(" (tentative)");
        }
        return sb.toString();
    }
}
