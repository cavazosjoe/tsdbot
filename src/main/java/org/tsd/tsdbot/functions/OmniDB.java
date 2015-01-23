package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.Persistable;
import org.tsd.tsdbot.util.DatabaseLogic;
import org.tsd.tsdbot.util.MiscUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Joe on 1/22/2015.
 */
@Singleton
public class OmniDB extends MainFunction implements Persistable {

    private static final Logger logger = LoggerFactory.getLogger(OmniDB.class);

    private static final String OMNIDB_TABLE_NAME = "OMNIDB";
    private static final String OMNIDB_TAG_TABLE_NAME = "OMNIDB_TAG";

    private DBConnectionProvider connectionProvider;
    private Random random;

    @Inject
    public OmniDB(TSDBot bot, DBConnectionProvider connectionProvider, Random random) throws SQLException {
        super(bot);
        this.description = "Use the patented TSD Omni Database";
        this.usage = "USAGE: .odb [ add #tag1 #tag2 <item> | get tag1 tag2 ]";
        this.connectionProvider = connectionProvider;
        this.random = random;
        initDB();
    }

    @Override
    public void initDB() throws SQLException {

        logger.info("Initializing Omni Database...");
        Connection connection = connectionProvider.get();

        if(!DatabaseLogic.tableExists(connection, OMNIDB_TABLE_NAME)) {

            logger.info("Table {} does NOT exist, creating...", OMNIDB_TABLE_NAME);

            String create = String.format("create table if not exists %s (" +
                    "id varchar not null," +
                    "data clob," +
                    "primary key (id))", OMNIDB_TABLE_NAME);

            try(PreparedStatement ps = connection.prepareStatement(create)) {
                logger.info("{}: {}", OMNIDB_TABLE_NAME, create);
                ps.executeUpdate();
            }

        }

        if(!DatabaseLogic.tableExists(connection, OMNIDB_TAG_TABLE_NAME)) {

            logger.info("Table {} does NOT exist, creating...", OMNIDB_TAG_TABLE_NAME);

            String create = String.format("create table if not exists %s (" +
                    "itemId varchar not null," +
                    "tag varchar," +
                    "foreign key (itemId) references OMNIDB(id))", OMNIDB_TAG_TABLE_NAME);

            try(PreparedStatement ps = connection.prepareStatement(create)) {
                logger.info("{}: {}", OMNIDB_TAG_TABLE_NAME, create);
                ps.executeUpdate();
            }

        }

        connection.commit();
    }

    @Override
    protected void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length == 1) {
            bot.sendMessage(channel, usage);
            return;
        }

        String subCmd = cmdParts[1];

        if(subCmd.equals("add")) {

            if(cmdParts.length == 2) {
                bot.sendMessage(channel, "Must provide something to add");
                return;
            }

            List<String> tags = new LinkedList<>();
            String item, word;
            StringBuilder itemBuilder = new StringBuilder();
            for(int i=2 ; i < cmdParts.length ; i++) {
                word = cmdParts[i];
                if(itemBuilder.length() > 0) {
                    // we are currently capturing
                    itemBuilder.append(word).append(" ");
                } else {
                    // we are not capturing, keep looking for tags
                    if(word.startsWith("#")) {
                        if(word.length() > 1) {
                            // this is a tag
                            tags.add(word.substring(1));
                        }
                    } else {
                        // this is the start of item, start recording
                        itemBuilder.append(word).append(" ");
                    }
                }
            }

            if(itemBuilder.length() == 0) {
                bot.sendMessage(channel, "Must provide something to add");
                return;
            } else {
                item = itemBuilder.toString().trim();
            }

            Connection connection = connectionProvider.get();

            String itemId = MiscUtils.getRandomString();
            String addItem = String.format("insert into %s (id, data) values (?,?)", OMNIDB_TABLE_NAME);
            try(PreparedStatement ps = connection.prepareStatement(addItem)) {
                ps.setString(1, itemId);
                ps.setString(2, item);
                ps.executeUpdate();
            } catch (Exception e) {
                logger.error("Error adding {} to omnidb", item, e);
                bot.sendMessage(channel, "Error adding item to Omni DB");
                return;
            }

            String addTag = String.format("insert into %s (itemId, tag) values (?,?)", OMNIDB_TAG_TABLE_NAME);
            for(String tag : tags) try(PreparedStatement ps = connection.prepareStatement(addTag)) {
                ps.setString(1, itemId);
                ps.setString(2, tag);
                ps.executeUpdate();
            } catch (Exception e) {
                logger.error("Error adding tag {} for itemId {}", new Object[]{tag, itemId}, e);
                bot.sendMessage(channel, "Error adding tag " + tag + " to Omni DB");
            }

            try {
                connection.commit();
                bot.sendMessage(channel, "Successfully added item to the Omni DB (" + itemId + ")");
            } catch (Exception e) {
                String msg = "Error committing to Omni DB";
                logger.error(msg, e);
                bot.sendMessage(channel, msg);
            }

        } else if(subCmd.equals("get")) {

            Connection connection = connectionProvider.get();

            String item = null;

            if(cmdParts.length == 2) {

                // empty get query, just return random data
                String q = String.format("select data from %s order by rand() limit 1", OMNIDB_TABLE_NAME);
                try(PreparedStatement ps = connection.prepareStatement(q)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            item = rs.getString(1);
                        }
                    }
                } catch(Exception e) {
                    String msg = "Error retrieving from Omni DB";
                    logger.error(msg, e);
                    bot.sendMessage(channel, msg);
                }

            } else {

                List<String> tags = new LinkedList<>();
                String word;
                // add all words as tags, but help them out if they prefix with hashtags by stripping them
                for (int i = 2; i < cmdParts.length; i++) {
                    word = cmdParts[i];
                    if (word.startsWith("#") && word.length() > 1) {
                        tags.add(word.substring(1));
                    } else if(!word.startsWith("#")) {
                        tags.add(word);
                    }
                }

                if (tags.isEmpty()) {
                    bot.sendMessage(channel, "Must provide some tags to fetch");
                    return;
                }

                String tagQueryPart = String.format("? in (select tag from %s where itemId = odb.id)", OMNIDB_TAG_TABLE_NAME);

                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append(String.format("select odb.data from %s odb where ", OMNIDB_TABLE_NAME));
                for(int i=0 ; i < tags.size() ; i++) {
                    if(i > 0)
                        queryBuilder.append(" and ");
                    queryBuilder.append(tagQueryPart);
                }
                queryBuilder.append(" order by rand() limit 1");

                try(PreparedStatement ps = connection.prepareStatement(queryBuilder.toString())) {

                    for(int i=0 ; i < tags.size() ; i++) {
                        ps.setString(i+1, tags.get(i));
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            item = rs.getString(1);
                        }
                    }

                } catch (Exception e) {
                    String msg = "Error retrieving from Omni DB";
                    logger.error(msg, e);
                    bot.sendMessage(channel, msg);
                }

            }

            if(item != null) {
                bot.sendMessage(channel, "ODB: " + item);
            } else {
                bot.sendMessage(channel, "Couldn't find anything in Omni DB matching those tags");
            }

        } else if(subCmd.equals("del")){

            if(!bot.getUserFromNick(channel, sender).hasPriv(User.Priv.HALFOP)) {
                bot.sendMessage(channel, "Only ops can use that");
                return;
            }

            if(cmdParts.length == 2) {
                bot.sendMessage(channel, "Must provide itemId to delete");
                return;
            }

            Connection connection = connectionProvider.get();
            String itemId = cmdParts[2];

            String deleteTags = String.format("delete from %s where itemId = ?", OMNIDB_TAG_TABLE_NAME);
            try(PreparedStatement ps = connection.prepareStatement(deleteTags)) {
                ps.setString(1, itemId);
                ps.executeUpdate();
            } catch (Exception e) {
                String msg = "Error deleting Omni DB tags";
                logger.error(msg, e);
                bot.sendMessage(channel, msg);
            }

            String deleteItem = String.format("delete from %s where id = ?", OMNIDB_TABLE_NAME);
            try(PreparedStatement ps = connection.prepareStatement(deleteItem)) {
                ps.setString(1, itemId);
                ps.executeUpdate();
            } catch (Exception e) {
                String msg = "Error deleting Omni DB entry";
                logger.error(msg, e);
                bot.sendMessage(channel, msg);
            }

            try {
                connection.commit();
                bot.sendMessage(channel, "Finished deleting entry " + itemId);
            } catch (Exception e) {
                String msg = "Error committing to Omni DB";
                logger.error(msg, e);
                bot.sendMessage(channel, msg);
            }

        } else {
            bot.sendMessage(channel, usage);
        }
    }

    @Override
    public String getRegex() {
        return "^\\.odb.*";
    }
}
