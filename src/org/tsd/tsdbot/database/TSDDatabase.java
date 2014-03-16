package org.tsd.tsdbot.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBotLauncher;
import org.tsd.tsdbot.TomCruise;

import java.io.*;
import java.sql.*;
import java.util.LinkedList;
import java.util.Properties;

public class TSDDatabase {

    private static Logger logger = LoggerFactory.getLogger("TSDDatabase");
    private static String testQ = "select 1";
    private static String connectionString = "jdbc:h2:tcp://localhost/" + System.getProperty("user.dir") + "/db";
    private Connection conn;

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("TSDDatabase init error", e);
        }
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

    private void initTSDTVDB() throws SQLException, IOException {

        // reload all blocks
        String blocksTable = "TSDTV_BLOCK";
        String dropBlocks = String.format("drop table if exists %s", blocksTable);
        try(PreparedStatement ps = getConnection().prepareStatement(dropBlocks)) {
            ps.execute();
        }

        String createBlocks = String.format("create table %s (" +
                "id int auto_increment," +
                "name varchar," +
                "quartzString varchar," +
                "primary key (id))", blocksTable);
        try(PreparedStatement ps = getConnection().prepareStatement(createBlocks)) {
            ps.executeUpdate();
        }

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
        InputStream fis = TSDBotLauncher.class.getResourceAsStream("/tsdbot.properties");
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
                            ps.executeUpdate();
                        }
                    }
                }
            }
        }

        // reload all episodes
        String episodesTable = "TSDTV_EPISODE";
        String dropEpisodes = String.format("drop table if exists %s", episodesTable);
        try(PreparedStatement ps = getConnection().prepareStatement(dropEpisodes)) {
            ps.execute();
        }

        String createEpisodes = String.format("create table %s (" +
                "blockId int," +
                "showId int," +
                "order int)" +
                blocksTable);
        try(PreparedStatement ps = getConnection().prepareStatement(createEpisodes)) {
            ps.executeUpdate();
        }

        FileInputStream schedule = new FileInputStream(new File(prop.getProperty("tsdtv.schedule")));
        try(BufferedReader br = new BufferedReader(new InputStreamReader(schedule))) {
            String line = null;
            while((line = br.readLine()) != null) {
                if(line.startsWith("BLOCK")) {
                    String blockName = line.substring(line.indexOf("=") + 1);
                    String quartzString = br.readLine();
                    LinkedList<String> shows = new LinkedList<>();
                    while(!(line = br.readLine()).equals("ENDBLOCK")) {
                        shows.add(line);
                    }

                    String newBlock = String.format(
                            "insert into %s (name, schedule) values ('%s', '%s')",
                            blocksTable, blockName, quartzString);
                    try(PreparedStatement ps = getConnection().prepareStatement(newBlock)) {
                        ps.executeUpdate();
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
