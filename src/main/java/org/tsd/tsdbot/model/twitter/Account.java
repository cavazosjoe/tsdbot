package org.tsd.tsdbot.model.twitter;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.tsd.tsdbot.model.BasicEntity;

@DatabaseTable(tableName = "TWITTER_ACCOUNT")
public class Account extends BasicEntity {

    @DatabaseField(canBeNull = false, unique = true)
    private long accountId;

    @DatabaseField(canBeNull = false)
    private String handle;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private String[] tags;

    @DatabaseField(canBeNull = false)
    private boolean throttled = false;

    public Account() {
    }

    public Account(long accountId) {
        this.accountId = accountId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public boolean isThrottled() {
        return throttled;
    }

    public void setThrottled(boolean throttled) {
        this.throttled = throttled;
    }
}
