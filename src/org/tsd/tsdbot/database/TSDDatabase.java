package org.tsd.tsdbot.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.functions.TomCruise;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class TSDDatabase {

    private static Logger logger = LoggerFactory.getLogger("TSDDatabase");
    private static String testQ = "select 1";
    private static String connectionString = "jdbc:h2:tcp://localhost/" + System.getProperty("user.dir") + "/db";
    private Connection conn;

    private static TSDDatabase instance = null;

    public static TSDDatabase getInstance() {
        if(instance == null) {
            instance = new TSDDatabase();
            instance.initialize();
        }
        return instance;
    }

    public Connection getConnection() {

        // create or find database at <current directory>/db/tsdbot
        // use tcp mode to allow concurrent admin connection
        try {
            if(conn == null || conn.isClosed())
                conn = DriverManager.getConnection(connectionString);
            try(PreparedStatement ps = conn.prepareStatement(testQ);ResultSet result = ps.executeQuery()) {}
        } catch (SQLException sqle) {
            System.err.println("db test query failed: " + sqle.getMessage());
            System.err.println("TRIED USING " + connectionString);
            return null;
        }

        return conn;

    }

    public void initialize() {
        try {
            initTomCruiseDb();
            if(!TSDBot.getInstance().isDebug()) {
                initTSDTVDB();
            }
        } catch (SQLException | IOException e) {
            logger.error("TSDDB init error",e);
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

    private void initTSDTVDB() throws SQLException, IOException {

        // load new shows
        String showsTable = "TSDTV_SHOW";
        String createShows = String.format("create table if not exists %s (" +
                "id int auto_increment," +
                "name varchar," +
                "currentEpisode int," +
                "primary key (id))", showsTable);
        try(PreparedStatement ps = getConnection().prepareStatement(createShows)) {
            ps.executeUpdate();
        }

        Properties prop = new Properties();
        InputStream fis = TSDDatabase.class.getResourceAsStream("/tsdbot.properties");
        prop.load(fis);
        String catalogPath = prop.getProperty("tsdtv.catalog");
        File catalogDir = new File(catalogPath);
        for(File f : catalogDir.listFiles()) {
            if(f.isDirectory()) {
                String q = String.format("select count(*) from %s where name = '%s'", showsTable, f.getName());
                try(PreparedStatement ps = getConnection().prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
                    result.next();
                    if(result.getInt(1) == 0) { // show does not exist in db, add it
                        String insertShow = String.format(
                                "insert into %s (name, currentEpisode) values ('%s',1)",
                                showsTable,
                                f.getName());
                        try(PreparedStatement ps1 = getConnection().prepareCall(insertShow)) {
                            ps1.executeUpdate();
                        }
                    }
                }
            }
        }

    }

    public boolean tableExists(String tableName) throws SQLException {
        String q = String.format("select count(*) from information_schema.tables where table_name = '%s'",tableName);
        try(PreparedStatement ps = getConnection().prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            result.next();
            return result.getInt(1) > 0;
        }
    }

}
