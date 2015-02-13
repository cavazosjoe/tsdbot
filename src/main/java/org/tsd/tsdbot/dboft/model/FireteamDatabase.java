package org.tsd.tsdbot.dboft.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.Persistable;
import org.tsd.tsdbot.model.dboft.Fireteam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Joe on 2/7/2015.
 */
@Singleton
public class FireteamDatabase implements Persistable {

    private static final Logger logger = LoggerFactory.getLogger(FireteamDatabase.class);

    @Inject
    private DBConnectionProvider connectionProvider;

    public Fireteam getFireteamForId(int id) {

        String q = "select * from DBO_FIRETEAM where id = ?";

        try(Connection connection = connectionProvider.get();
            PreparedStatement ps = connection.prepareStatement(q)) {

            ps.setInt(1, id);
            ResultSet result = ps.executeQuery();
            while(result.next()) {
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void initDB() throws SQLException {

        Connection connection = connectionProvider.get();

        String fireteamTableName = "DBO_FIRETEAM";
        String createFireteamTable = String.format(
                "create table if not exists %s (" +
                        "id             int," +
                        "platform       varchar," +
                        "activity       varchar," +
                        "title          varchar," +
                        "eventTime      timestamp," +
                        "description    varchar," +
                        "dboUserId      int," + // -> DboUser
                        "url            varchar," +
                        "deleted        boolean," +
                        "primary key (id) )", fireteamTableName
        );

        try(PreparedStatement ps = connection.prepareStatement(createFireteamTable)) {
            logger.info("{}: {}", fireteamTableName, createFireteamTable);
            ps.executeUpdate();
        }

        String dboUserTableName = "DBO_USER";
        String createDboUserTable = String.format(
                "create table if not exists %s (" +
                        "id     int," +
                        "handle varchar," +
                        "primary key (id) )", dboUserTableName
        );

        try(PreparedStatement ps = connection.prepareStatement(createDboUserTable)) {
            logger.info("{}: {}", dboUserTableName, createDboUserTable);
            ps.executeUpdate();
        }

        String rsvpTableName = "DBO_FIRETEAM_RSVP";
        String createRsvpTable = String.format(
                "create table if not exists %s (" +
                        "id             int auto_increment," +
                        "fireteamId     int," + // -> Fireteam
                        "creatorId      int," + // -> DboUser
                        "gamertag       varchar," +
                        "characterClass varchar," +
                        "level          int," +
                        "comment        varchar," +
                        "tentative      boolean," +
                        "primary key (id) )", rsvpTableName
        );

        try(PreparedStatement ps = connection.prepareStatement(createRsvpTable)) {
            logger.info("{}: {}", rsvpTableName, createRsvpTable);
            ps.executeUpdate();
        }
    }

    @Override
    public void initDB2(JdbcConnectionSource connectionSource) {

    }
}
