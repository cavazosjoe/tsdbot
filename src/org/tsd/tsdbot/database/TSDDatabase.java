package org.tsd.tsdbot.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TSDDatabase {

    static {
        try {
            Class.forName("org.h2.Driver");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {

        // create or find database at <current directory>/db/tsdbot
        // use tcp mode to allow concurrent admin connection
        String connectionString = "jdbc:h2:tcp://localhost/" + System.getProperty("user.dir") + "/db/tsdbot";
        return DriverManager.getConnection(connectionString);
    }

    public static void initialize() {
        try (Connection conn = getConnection()) {

            ensureTomCruiseQuotes(conn);
            ensureTomCruiseClips(conn);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void ensureTomCruiseQuotes(Connection conn) {

        String create = "create table if not exists TomCruiseQuotes ( id int auto_increment, quote text, primary key (id) )";

        try (PreparedStatement ps = conn.prepareStatement(create)) {
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void ensureTomCruiseClips(Connection conn) {

        String create = "create table if not exists TomCruiseClips ( id int auto_increment, clip text, primary key (id) )";

        try (PreparedStatement ps = conn.prepareStatement(create)) {
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
