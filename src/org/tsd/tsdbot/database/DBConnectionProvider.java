package org.tsd.tsdbot.database;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Created by Joe on 9/20/2014.
 */
@Singleton
public class DBConnectionProvider implements Provider<Connection> {

    private static Logger logger = LoggerFactory.getLogger(DBConnectionProvider.class);

    private static String testQuery = "select 1";

    private Connection connection;
    private String connectionString;

    @Inject
    public DBConnectionProvider(@DBConnectionString String connectionString) {
        logger.info("Initializing ConnectionProvider with connectionString={}", connectionString);
        this.connectionString = connectionString;
    }

    @Override
    public Connection get() {
        // use tcp mode to allow concurrent admin connection
        try {
            if(connection == null || connection.isClosed()) {
                logger.info("Connection is null or closed, retrying with connectionString={}", connectionString);
                connection = DriverManager.getConnection(connectionString);
            }
            try(PreparedStatement ps = connection.prepareStatement(testQuery);ResultSet result = ps.executeQuery()) {}
        } catch (SQLException sqle) {
            logger.error("DB TEST QUERY FAILED: " + sqle.getMessage(), sqle);
            return null;
        }

        return connection;
    }
}
