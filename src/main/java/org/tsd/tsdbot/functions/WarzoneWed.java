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
import org.tsd.tsdbot.database.JdbcConnectionProvider;
import org.tsd.tsdbot.model.warzone.WarzoneRegular;
import org.tsd.tsdbot.module.Function;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
@Function(initialRegex = "^\\.ww.*")
public class WarzoneWed extends MainFunctionImpl {

    private static final Logger log = LoggerFactory.getLogger(WarzoneWed.class);

    private static final Pattern gameIdPattern = Pattern.compile("warzone/matches/(.*?)[/?]", Pattern.DOTALL);
    private static final int startHour = 17; // 5PM Pacific

    private final JdbcConnectionProvider connectionProvider;

    // feed in game links one at a time, then process
    private final HashSet<String> gameIds = new HashSet<>();

    @Inject
    public WarzoneWed(Bot bot, JdbcConnectionProvider connectionProvider) {
        super(bot);
        this.connectionProvider = connectionProvider;
        this.description = "They said it couldn't be done. They were wrong, mostly.";
        this.usage = "USAGE: .ww link1 [link2 link 3...]";
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        if(!bot.userHasGlobalPriv(sender, User.Priv.OWNER)) {
            bot.sendMessage(channel, "Only owners can use that");
            return;
        }

        String[] parts = text.split("\\s+");
        if(parts.length < 2) {
            bot.sendMessage(channel, "USAGE: .ww [regulars GT1=Handle1, GT2=Handle2, ...] [game link] [process]");
            return;
        }

        JdbcConnectionSource connectionSource = null;
        try {
            connectionSource = connectionProvider.get();
            if(parts[1].equalsIgnoreCase("regulars")) {
                // adding regulars to the database in format Gamertag=Handle
                // e.g. .ww regulars SomeGamertag=Some Handle, GT2=HaNdLe2, ...
                RegularsResult result = addRegulars(connectionSource, text);
                bot.sendMessage(channel, "Created " + result.created + " regulars and updated " + result.updated);
            }

            else if(parts[1].equals("process")) {
                processGames();
            }

            else {
                String url = parts[1];
                Matcher gameIdMatcher = gameIdPattern.matcher(url);
                String gameId = null;
                while(gameIdMatcher.find()) {
                    gameId = gameIdMatcher.group(1);
                }

                if(gameId == null) {
                    throw new Exception("Could not determine Game ID from URL " + url);
                } else {
                    gameIds.add(gameId);
                }
            }

        } catch (Exception e) {
            bot.sendMessage(channel, "Error: " + e.getMessage());
        } finally {
            if(connectionSource != null)
                connectionSource.closeQuietly();
        }
    }

    void processGames() throws Exception {

        // detect the time of this WW night and check for an existing night
        // (delete if necessary)
        Calendar calendar = Calendar.getInstance();
        if(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
            // It's currently wednesday. If we're before the usual start time, this processing batch
            // is for the previous wednesday
            if(calendar.get(Calendar.HOUR_OF_DAY) < startHour) {
                // bump the calendar off wednesday
                calendar.add(Calendar.DATE, -1);
            }
        }

        while(calendar.get(Calendar.DAY_OF_WEEK) != Calendar.WEDNESDAY) {
            calendar.add(Calendar.DATE, -1);
        }

        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date time = calendar.getTime();


    }

    RegularsResult addRegulars(JdbcConnectionSource connectionSource, String rawLine) throws Exception {

        log.info("Adding regulars using rawline: {}", rawLine);
        String[] parts = rawLine.split(",");

        // the first token will be ".ww regulars GT=Handle" so trim it
        parts[0] = parts[0].split("\\s+", 3)[2].trim();

        Dao<WarzoneRegular, String> regularDao = DaoManager.createDao(connectionSource, WarzoneRegular.class);
        String handle;
        String gt;
        String[] tokens;
        WarzoneRegular regular;
        RegularsResult result = new RegularsResult();
        for(String keyValue : parts) {
            log.info("Evaluating token {}...", keyValue);
            tokens = keyValue.split("=");
            gt = tokens[0].trim();
            handle = tokens[1].trim();

            regular = regularDao.queryForId(gt);
            if(regular == null) {
                log.info("Did not find regular with GT {} in database, creating...", gt);
                regular = new WarzoneRegular();
                regular.setGamertag(gt);
                regular.setForumHandle(handle);
                regularDao.create(regular);
                result.addCreated();
            } else {
                log.info("Found regular with GT {} in database, updating handle to {}...", gt, handle);
                regular.setForumHandle(handle);
                regularDao.update(regular);
                result.addUpdated();
            }
        }

        return result;

    }

    class RegularsResult {
        public int created = 0;
        public int updated = 0;

        public void addCreated() { created++; }
        public void addUpdated() { updated++; }
    }
}
