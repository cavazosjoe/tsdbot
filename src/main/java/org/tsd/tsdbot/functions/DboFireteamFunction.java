package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.Function;
import org.tsd.tsdbot.database.JdbcConnectionProvider;
import org.tsd.tsdbot.model.dboft.Fireteam;

import java.sql.SQLException;

/**
 * Created by Joe on 2/7/2015.
 */
@Singleton
@Function(initialRegex = "^\\.dboft.*")
public class DboFireteamFunction extends MainFunctionImpl {

    private static final Logger logger = LoggerFactory.getLogger(DboFireteamFunction.class);

    private JdbcConnectionProvider connectionProvider;

    @Inject
    public DboFireteamFunction(Bot bot, JdbcConnectionProvider connectionProvider) {
        super(bot);
        this.description = "DBO Fireteam function. Manage subscriptions to DBO Fireteam notifications";
        this.usage = "USAGE: .dboft [ subscribe <id> | unsubscribe <id> ]";
        this.bot = bot;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length < 3) {
            bot.sendMessage(channel, usage);
            return;
        }

        JdbcConnectionSource connectionSource = null;

        try {
            connectionSource = connectionProvider.get();
            Dao<Fireteam, Integer> fireteamDao = DaoManager.createDao(connectionSource, Fireteam.class);

            if (cmdParts[1].equals("subscribe") || cmdParts[1].equals("unsubscribe")) {

                Integer fireteamId;
                try {
                    fireteamId = Integer.parseInt(cmdParts[2]);
                } catch (NumberFormatException nfe) {
                    logger.error("Error parsing fireteam id {}", cmdParts[2]);
                    bot.sendMessage(channel, "Fireteam ID must be an integer");
                    return;
                }

                Fireteam fireteam = fireteamDao.queryForId(fireteamId);
                if(fireteam == null) {
                    bot.sendMessage(channel, "Could not find Fireteam with id " + fireteamId);
                    return;
                }

                boolean subscribing = cmdParts[1].equals("subscribe");

                if(subscribing) {

                    if(fireteam.isSubscribed()) {
                        bot.sendMessage(channel, "Already subscribed to " + fireteam.getEffectiveTitle() + " (id=" + fireteamId + ")");
                    } else {
                        fireteam.setSubscribed(true);
                        fireteamDao.update(fireteam);
                        bot.sendMessage(channel, "Now subscribed to " + fireteam.getEffectiveTitle() + " (id=" + fireteamId + ")");
                    }

                } else {

                    if(!bot.userHasPrivInChannel(sender, channel, User.Priv.OP)) {
                        bot.sendMessage(channel, "Only an op can unsubscribe from notifications");
                        return;
                    }

                    if(!fireteam.isSubscribed()) {
                        bot.sendMessage(channel, "Not currently subscribed to " + fireteam.getEffectiveTitle() + " (id=" + fireteamId + ")");
                    } else {
                        fireteam.setSubscribed(false);
                        fireteamDao.update(fireteam);
                        bot.sendMessage(channel, "No longer subscribed to " + fireteam.getEffectiveTitle() + " (id=" + fireteamId + ")");
                    }

                }

            }

        } catch (SQLException e) {
            logger.error("Error getting connection to database", e);
            bot.sendMessage(channel, "Error communicating with database");
        } finally {
            if(connectionSource != null)
                connectionSource.closeQuietly();
        }
    }

}
