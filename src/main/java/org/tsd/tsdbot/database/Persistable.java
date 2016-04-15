package org.tsd.tsdbot.database;

import java.sql.SQLException;

/**
 * Tags a class as having associated table(s) in the database
 */
public interface Persistable {
    void initDB() throws SQLException;
}
