package org.tsd.tsdbot.database;

import org.tsd.tsdbot.TomCruise;

import java.sql.*;

public class TSDDatabase {

    private static String testQ = "select 1";
    private static String connectionString = "jdbc:h2:tcp://localhost/" + System.getProperty("user.dir") + "/db";
    private Connection conn;

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {

        // create or find database at <current directory>/db/tsdbot
        // use tcp mode to allow concurrent admin connection
        try {
            if(conn == null || conn.isClosed())
                conn = DriverManager.getConnection(connectionString);

            PreparedStatement ps = conn.prepareStatement(testQ);
            ps.executeQuery();
        } catch (SQLException sqle) {
            System.err.println("db test query failed: " + sqle.getMessage());
            return null;
        }

        return conn;

    }

    public void initialize() {
        try {
            initTomCruiseDb();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initTomCruiseDb() throws SQLException {
        String tomCruiseTable = "TOMCRUISE";
        if(!tableExists(tomCruiseTable)) {

            String create = String.format("create table if not exists %s (" +
                    "id int auto_increment," +
                    "type varchar," +
                    "data clob," +
                    "primary key (id))",tomCruiseTable);

            try(PreparedStatement ps = getConnection().prepareStatement(create)) {
                ps.executeUpdate();
            }

            StringBuilder insertBuilder = new StringBuilder();
            insertBuilder.append(String.format("insert into %s (type, data) values ",tomCruiseTable));
            boolean first = true;
            for(String quote : TomCruise.quotes) {
                if(!first) insertBuilder.append(",");
                insertBuilder.append(String.format("('quote','%s')",quote));
                first = false;
            }
            for(String quote : TomCruise.clips) {
                if(!first) insertBuilder.append(",");
                insertBuilder.append(String.format("('clip','%s')",quote));
                first = false;
            }

            try(PreparedStatement ps = getConnection().prepareCall(insertBuilder.toString())) {
                ps.executeUpdate();
            }

            conn.commit();

        }
    }


//    public static void ensureTomCruiseQuotes(Connection conn) {
//
//        String create = "create table if not exists TomCruiseQuotes ( id int auto_increment, quote text, primary key (id) )";
//
//        try (PreparedStatement ps = conn.prepareStatement(create)) {
//            ps.executeUpdate();
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public static void ensureTomCruiseClips(Connection conn) {
//
//        String create = "create table if not exists TomCruiseClips ( id int auto_increment, clip text, primary key (id) )";
//
//        try (PreparedStatement ps = conn.prepareStatement(create)) {
//            ps.executeUpdate();
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }

    public boolean tableExists(String tableName) throws SQLException {
        String q = String.format("select count(*) from information_schema.tables where table_name = '%s'",tableName);
        PreparedStatement ps = getConnection().prepareStatement(q);
        try(ResultSet result = ps.executeQuery()) {
            result.next();
            return result.getInt(1) > 0;
        }
    }

}
