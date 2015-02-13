package org.tsd.tsdbot.model.dboft;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by Joe on 2/7/2015.
 */
@DatabaseTable(tableName = "DBO_USER")
public class DboUser {

    @DatabaseField(id = true)
    private int id;

    @DatabaseField
    private String handle;

    @Deprecated
    public DboUser() {}

    public DboUser(int id, String handle) {
        this.id = id;
        this.handle = handle;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DboUser dboUser = (DboUser) o;

        if (id != dboUser.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
