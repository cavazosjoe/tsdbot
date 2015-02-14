package org.tsd.tsdbot.model.dboft;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.commons.lang3.StringUtils;
import org.tsd.tsdbot.util.IRCUtil;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by Joe on 2/7/2015.
 */
@DatabaseTable(tableName = "DBO_FIRETEAM")
public class Fireteam {

    @DatabaseField(id = true)
    private int id;

    @DatabaseField(canBeNull = false)
    private Platform platform;

    @DatabaseField
    private String activity;

    @DatabaseField
    private Difficulty difficulty;

    @DatabaseField
    private Integer level;

    @DatabaseField
    private String title;

    @DatabaseField(canBeNull = false)
    private Date eventTime;

    @DatabaseField(columnDefinition = "CLOB")
    private String description;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = "creatorId", canBeNull = false)
    private DboUser creator;

    @DatabaseField
    private boolean deleted;

    @ForeignCollectionField
    private ForeignCollection<FireteamRSVP> rsvps;

    @Deprecated
    public Fireteam() {}

    public Fireteam(int id) {
        this.id = id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DboUser getCreator() {
        return creator;
    }

    public void setCreator(DboUser creator) {
        this.creator = creator;
    }

    public ForeignCollection<FireteamRSVP> getRsvps() {
        return rsvps;
    }

    public void setRsvps(ForeignCollection<FireteamRSVP> rsvps) {
        this.rsvps = rsvps;
    }

    public String getUrl() {
        return "http://destiny.bungie.org/forum/index.php?mode=fireteambuilder&event=" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fireteam fireteam = (Fireteam) o;

        if (id != fireteam.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm z");
        sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        StringBuilder sb = new StringBuilder();

        IRCUtil.IRCColor platformBgColor = null;
        switch(platform) {
            case x360:
            case xb1: platformBgColor = IRCUtil.IRCColor.green; break;
            case ps3:
            case ps4: platformBgColor = IRCUtil.IRCColor.blue; break;
        }
        String coloredPlatformString = IRCUtil.color("[" + platform.getDisplayString() + "]", IRCUtil.IRCColor.white, platformBgColor);
        sb.append(coloredPlatformString);

        sb.append(" (").append(creator.getHandle()).append(") ");
        if (StringUtils.isEmpty(title)) {
            if(activity != null)
                sb.append(IRCUtil.bold(activity));
            if(difficulty != null)
                sb.append(", ").append(difficulty);
        } else {
            sb.append(IRCUtil.bold(title));
            if(activity != null) {
                sb.append(" (").append(activity);
                if (difficulty != null)
                    sb.append(", ").append(difficulty);
                sb.append(")");
            }
        }

        sb.append(" ").append(sdf.format(eventTime));

        return sb.toString();
    }
}
