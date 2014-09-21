package org.tsd.tsdbot.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.functions.TomCruise;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

@Singleton
public class TSDDatabase {

    private static Logger logger = LoggerFactory.getLogger(TSDDatabase.class);

    private DBConnectionProvider connectionProvider;

    @Inject
    public TSDDatabase(DBConnectionProvider connectionProvider, TSDBot bot) {
        this.connectionProvider = connectionProvider;

        try {
            initTomCruiseDb();
            if(!bot.isDebug())
                initTSDTVDB();
        } catch (SQLException | IOException e) {
            logger.error("TSDDB init error", e);
            bot.broadcast("Error initializing database, please check logs");
        }
    }

    private void initTomCruiseDb() throws SQLException {
        String tomCruiseTable = "TOMCRUISE";
        if(!tableExists(tomCruiseTable)) {

            Connection connection = connectionProvider.get();

            String create = String.format("create table if not exists %s (" +
                    "id int auto_increment," +
                    "type varchar," +
                    "data clob," +
                    "primary key (id))",tomCruiseTable);

            try(PreparedStatement ps = connection.prepareStatement(create)) {
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

            try(PreparedStatement ps = connection.prepareCall(insertBuilder.toString())) {
                ps.executeUpdate();
            }

            connection.commit();
        }
    }

    private void initTSDTVDB() throws SQLException, IOException {

        Connection connection = connectionProvider.get();

        // load new shows
        String showsTable = "TSDTV_SHOW";
        String createShows = String.format("create table if not exists %s (" +
                "id int auto_increment," +
                "name varchar," +
                "currentEpisode int," +
                "primary key (id))", showsTable);
        try(PreparedStatement ps = connection.prepareStatement(createShows)) {
            ps.executeUpdate();
        }

        try(InputStream fis = TSDDatabase.class.getResourceAsStream("/tsdbot.properties")) {
            Properties prop = new Properties();
            prop.load(fis);
            String catalogPath = prop.getProperty("tsdtv.catalog");
            File catalogDir = new File(catalogPath);
            for(File f : catalogDir.listFiles()) {
                if(f.isDirectory()) {
                    String q = String.format("select count(*) from %s where name = '%s'", showsTable, f.getName());
                    try(PreparedStatement ps = connection.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
                        result.next();
                        if(result.getInt(1) == 0) { // show does not exist in db, add it
                            String insertShow = String.format(
                                    "insert into %s (name, currentEpisode) values ('%s',1)",
                                    showsTable,
                                    f.getName());
                            try(PreparedStatement ps1 = connection.prepareCall(insertShow)) {
                                ps1.executeUpdate();
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean tableExists(String tableName) throws SQLException {
        Connection connection = connectionProvider.get();
        String q = String.format("select count(*) from information_schema.tables where table_name = '%s'",tableName);
        try(PreparedStatement ps = connection.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            result.next();
            return result.getInt(1) > 0;
        }
    }

}
