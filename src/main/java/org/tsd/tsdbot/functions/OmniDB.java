package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.ArrayUtils;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.Persistable;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.util.DatabaseLogic;
import org.tsd.tsdbot.util.MiscUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Function(initialRegex = "^\\.odb.*")
public class OmniDB extends MainFunctionImpl implements Persistable {

    private static final Logger logger = LoggerFactory.getLogger(OmniDB.class);

    private static final String OMNIDB_TABLE_NAME = "OMNIDB";
    private static final String OMNIDB_TAG_TABLE_NAME = "OMNIDB_TAG";

    private static final String INCLUDE_TAGS = "-includetags";
    private static final String INCLUDE_ID = "-includeid";

    private static final Set<String> modifiers = new HashSet<>(
            Arrays.asList(INCLUDE_TAGS, INCLUDE_ID)
    );

    private final DBConnectionProvider connectionProvider;
    private final Map<String, Item> lastPulledItems = new ConcurrentHashMap<>();

    @Inject
    public OmniDB(Bot bot, DBConnectionProvider connectionProvider) throws SQLException {
        super(bot);
        this.description = "Use the patented TSD Omni Database";
        this.usage = "USAGE: .odb [ add #tag1 #tag2 <item> | get tag1 tag2 ]";
        this.connectionProvider = connectionProvider;
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
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length == 1 || (cmdParts.length == 2 && cmdParts[1].equals("get")) ) {
            try {
                Item item = getAnyItem();
                printItem(item, true, false, channel);
                lastPulledItems.put(channel, item);
            } catch (Exception e) {
                String msg = "Error retrieving from Omni DB";
                logger.error(msg, e);
                bot.sendMessage(channel, msg);
            }
            return;
        }

        String subCmd = cmdParts[1];

        switch (subCmd) {
            case "add": {

                if (cmdParts.length == 2) {
                    bot.sendMessage(channel, "Must provide something to add");
                    return;
                }

                List<String> tags = new LinkedList<>();
                String item, word;
                StringBuilder itemBuilder = new StringBuilder();
                for (int i = 2; i < cmdParts.length; i++) {
                    word = cmdParts[i];
                    if (itemBuilder.length() > 0) {
                        // we are currently capturing
                        itemBuilder.append(word).append(" ");
                    } else {
                        // we are not capturing, keep looking for tags
                        if (word.startsWith("#")) {
                            if (word.length() > 1) {
                                // this is a tag
                                tags.add(word.substring(1));
                            }
                        } else {
                            // this is the start of item, start recording
                            itemBuilder.append(word).append(" ");
                        }
                    }
                }

                if (itemBuilder.length() == 0) {
                    bot.sendMessage(channel, "Must provide something to add");
                } else try {
                    item = itemBuilder.toString().trim();
                    String itemId = addItem(item, tags);
                    bot.sendMessage(channel, "Successfully added item to the Omni DB ( " + itemId + " )");
                } catch (Exception e) {
                    String msg = "Error adding to Omni DB";
                    logger.error(msg, e);
                    bot.sendMessage(channel, msg);
                }

                break;
            }

            case "del": {

                if (!bot.userHasGlobalPriv(sender, User.Priv.HALFOP)) {
                    bot.sendMessage(channel, "Only ops can use that");
                    return;
                }

                if (cmdParts.length == 2) {
                    bot.sendMessage(channel, "Must provide itemId to delete");
                    return;
                }

                String itemId = cmdParts[2];
                deleteItem(channel, itemId);

                break;
            }

            case "size": {
                printSize(channel);
                break;
            }

            case "use":
            case "usage": {
                bot.sendMessage(channel, usage);
                break;
            }

            case "runback": {
                Item lastPulled = lastPulledItems.get(channel);
                if(lastPulled == null) {
                    bot.sendMessage(channel, "No ODB pulls in this channel to run back");
                } else {
                    printItem(lastPulled, true, true, channel);
                }
                break;
            }

            case "get":
            default: {

                boolean includeTags = ArrayUtils.contains(cmdParts, INCLUDE_TAGS);
                boolean includeId = ArrayUtils.contains(cmdParts, INCLUDE_ID);

                Item item = null;
                List<String> tags = buildTagList(cmdParts);
                if (tags.isEmpty()) {
                    bot.sendMessage(channel, "Must provide some tags to fetch");
                    return;
                }

                try {
                    item = getTaggedItem(tags);
                } catch (Exception e) {
                    String msg = "Error retrieving from Omni DB";
                    logger.error(msg, e);
                    bot.sendMessage(channel, msg);
                }

                if (item != null) {
                    printItem(item, includeTags, includeId, channel);
                    lastPulledItems.put(channel, item);
                } else {
                    bot.sendMessage(channel, "Couldn't find anything in Omni DB matching those tags");
                }

                break;
            }
        }
    }

    List<String> buildTagList(String[] cmdParts) {
        List<String> tags = new LinkedList<>();
        String word;

        // .odb UH OH --> start compiling tags at index [1]
        // .odb get UH OH --> start compiling tags at index [2]
        int startIdx = cmdParts[1].equals("get") ? 2 : 1;

        // add all words as tags, but help them out if they prefix with hashtags by stripping them
        for (int i = startIdx; i < cmdParts.length; i++) {
            word = cmdParts[i];
            if (word.startsWith("#") && word.length() > 1) {
                tags.add(word.substring(1));
            } else if ( (!modifiers.contains(word)) && !word.startsWith("#")) {
                tags.add(word);
            }
        }
        return tags;
    }

    void printItem(Item item, boolean includeTags, boolean includeId, String channel) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("ODB: ").append(item.item);
            if (includeTags) {
                for (String tag : getTagsForItem(item.itemId)) {
                    sb.append(" #").append(tag);
                }
            }
            if (includeId) {
                sb.append(" (id = ").append(item.itemId).append(" )");
            }
            bot.sendMessage(channel, sb.toString());
        } catch (Exception e) {
            String msg = "Error getting tags for item";
            logger.error(msg, e);
            bot.sendMessage(channel, msg);
        }
    }

    String addItem(String item, List<String> tags) throws SQLException {
        Connection connection = connectionProvider.get();

        String itemId = MiscUtils.getRandomString();
        String addItem = String.format("insert into %s (id, data) values (?,?)", OMNIDB_TABLE_NAME);
        try(PreparedStatement ps = connection.prepareStatement(addItem)) {
            ps.setString(1, itemId);
            ps.setString(2, item);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error adding {} to omnidb", item, e);
            throw e;
        }

        String addTag = String.format("insert into %s (itemId, tag) values (?,?)", OMNIDB_TAG_TABLE_NAME);
        for(String tag : tags) try(PreparedStatement ps = connection.prepareStatement(addTag)) {
            ps.setString(1, itemId);
            ps.setString(2, tag);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Error adding tag {} for itemId {}", new Object[]{tag, itemId}, e);
            throw e;
        }

        connection.commit();

        return itemId;
    }

    Item getAnyItem() throws SQLException {
        Connection connection = connectionProvider.get();
        // empty get query, just return random data
        String q = String.format("select id, data from %s order by rand() limit 1", OMNIDB_TABLE_NAME);
        try(PreparedStatement ps = connection.prepareStatement(q)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Item(rs.getString(1), rs.getString(2));
                }
            }
        }
        return null;
    }

    Item getTaggedItem(List<String> tags) throws SQLException {
        Connection connection = connectionProvider.get();
        String tagQueryPart = String.format("? in (select tag from %s where itemId = odb.id)", OMNIDB_TAG_TABLE_NAME);
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(String.format("select odb.id, odb.data from %s odb where ", OMNIDB_TABLE_NAME));
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
                    return new Item(rs.getString(1), rs.getString(2));
                }
            }
        }
        return null;
    }

    void deleteItem(String channel, String itemId) {
        Connection connection = connectionProvider.get();
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
    }

    Set<String> getTagsForItem(String itemId) throws SQLException {
        String getTags = String.format("select tag from %s where itemId = ?", OMNIDB_TAG_TABLE_NAME);
        Set<String> tags = new HashSet<>();
        try(PreparedStatement ps = connectionProvider.get().prepareStatement(getTags)) {
            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(rs.getString(1));
                }
            }
        }
        return tags;
    }

    void printSize(String channel) {
        Connection connection = connectionProvider.get();

        int numItems = 0;
        String itemSizeQ = String.format("select count(*) from %s", OMNIDB_TABLE_NAME);
        try(PreparedStatement ps = connection.prepareStatement(itemSizeQ)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    numItems = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            String msg = "Error retrieving size of item table";
            logger.error(msg, e);
            bot.sendMessage(channel, msg);
        }

        int numTags = 0;
        String numTagsQ = String.format("select count(*) from %s", OMNIDB_TAG_TABLE_NAME);
        try(PreparedStatement ps = connection.prepareStatement(numTagsQ)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    numTags = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            String msg = "Error retrieving size of tag table";
            logger.error(msg, e);
            bot.sendMessage(channel, msg);
        }

        bot.sendMessage(channel, String.format("Currently %s items and %s tags in the Omni DB", numItems, numTags));
    }

    static class Item {
        public String itemId;
        public String item;

        Item(String itemId, String item) {
            this.itemId = itemId;
            this.item = item;
        }
    }
}
