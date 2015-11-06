package org.tsd.tsdbot.model.warzone;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by Joe on 10/31/2015.
 */
@DatabaseTable(tableName = "WarzoneRegular")
public class WarzoneRegular {

    @DatabaseField(id = true)
    private String gamertag;

    @DatabaseField
    private String forumHandle;

    @ForeignCollectionField(foreignFieldName = "regular")
    private ForeignCollection<WarzoneGamePlayer> games;

    public String getGamertag() {
        return gamertag;
    }

    public void setGamertag(String gamertag) {
        this.gamertag = gamertag;
    }

    public String getForumHandle() {
        return forumHandle;
    }

    public void setForumHandle(String forumHandle) {
        this.forumHandle = forumHandle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WarzoneRegular regular = (WarzoneRegular) o;

        return !(gamertag != null ? !gamertag.equals(regular.gamertag) : regular.gamertag != null);

    }

    @Override
    public int hashCode() {
        return gamertag != null ? gamertag.hashCode() : 0;
    }

    public ForeignCollection<WarzoneGamePlayer> getGames() {
        return games;
    }

    public void setGames(ForeignCollection<WarzoneGamePlayer> games) {
        this.games = games;
    }
}
