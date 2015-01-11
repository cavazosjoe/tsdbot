package org.tsd.tsdbot.database;

import java.sql.SQLException;

/**
 * Tags a class as having associated table(s) in the database
 */
public interface Persistable {
    public void initDB() throws SQLException;
}
