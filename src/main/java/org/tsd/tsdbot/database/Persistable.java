package org.tsd.tsdbot.database;

import com.j256.ormlite.jdbc.JdbcConnectionSource;

import java.sql.SQLException;

/**
 * Tags a class as having associated table(s) in the database
 */
public interface Persistable {
    public void initDB() throws SQLException;
    public void initDB2(JdbcConnectionSource connectionSource);
}
