package org.tsd.tsdbot.database;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Created by Joe on 9/20/2014.
 */
public class DBConnectionProvider implements Provider<Connection> {

    private static Logger logger = LoggerFactory.getLogger(DBConnectionProvider.class);

    private static String testQuery = "select 1";

    private Connection connection;
    private String connectionString;

    @Inject
    public DBConnectionProvider(@DBConnectionString String connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public Connection get() {
        // create or find database at <current directory>/db/tsdbot
        // use tcp mode to allow concurrent admin connection
        try {
            if(connection == null || connection.isClosed())
                connection = DriverManager.getConnection(connectionString);
            try(PreparedStatement ps = connection.prepareStatement(testQuery);ResultSet result = ps.executeQuery()) {}
        } catch (SQLException sqle) {
            logger.error("DB TEST QUERY FAILED: " + sqle.getMessage(), sqle);
            logger.error("CONNECTION STRING: " + connectionString);
            return null;
        }

        return connection;
    }
}
