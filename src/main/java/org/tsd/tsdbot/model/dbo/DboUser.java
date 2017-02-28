package org.tsd.tsdbot.model.dbo;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.tsd.tsdbot.model.BasicEntity;

@DatabaseTable(tableName = "DBO_USER")
public class DboUser extends BasicEntity {

    @DatabaseField(canBeNull = false, unique = true)
    private int userId;

    @DatabaseField
    private String handle;

    @Deprecated
    public DboUser() {}

    public DboUser(int id, String handle) {
        this.userId = id;
        this.handle = handle;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }
}
