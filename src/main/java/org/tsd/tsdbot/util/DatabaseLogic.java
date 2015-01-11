package org.tsd.tsdbot.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Joe on 9/21/2014.
 */
public class DatabaseLogic {

    public static boolean tableExists(Connection connection, String tableName) throws SQLException {
        String q = String.format("select count(*) from information_schema.tables where table_name = '%s'",tableName);
        try(PreparedStatement ps = connection.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            result.next();
            return result.getInt(1) > 0;
        }
    }
}
