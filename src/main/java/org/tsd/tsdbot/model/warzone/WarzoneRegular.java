package org.tsd.tsdbot.model.warzone;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.tsd.tsdbot.model.BasicEntity;

@DatabaseTable(tableName = "WarzoneRegular")
public class WarzoneRegular extends BasicEntity {

    @DatabaseField(canBeNull = false, unique = true)
    private String gamertag;

    @DatabaseField
    private String forumHandle;

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
}
