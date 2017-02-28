package org.tsd.tsdbot.model.warzone;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import org.tsd.tsdbot.model.BasicEntity;

import java.util.Date;

@DatabaseTable(tableName = "WarzoneNight")
public class WarzoneNight extends BasicEntity {

    @DatabaseField(canBeNull = false)
    private Date date;

    @ForeignCollectionField
    private ForeignCollection<WarzoneGame> games;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public ForeignCollection<WarzoneGame> getGames() {
        return games;
    }

    public void setGames(ForeignCollection<WarzoneGame> games) {
        this.games = games;
    }
}
