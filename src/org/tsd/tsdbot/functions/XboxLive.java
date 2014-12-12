package org.tsd.tsdbot.functions;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jibble.pircbot.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.history.MessageFilter;
import org.tsd.tsdbot.history.MessageFilterStrategy;
import org.tsd.tsdbot.util.FuzzyLogic;
import org.tsd.tsdbot.util.IRCUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class XboxLive extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Printout.class);

    // list of all friends for given XUID
    private static final String FRIENDS_TARGET = "https://xboxapi.com/v2/%d/friends";

    // status information for a given XUID
    private static final String PRESENCE_TARGET = "https://xboxapi.com/v2/%d/presence";

    // recent activity for a given XUID
    private static final String ACTIVITY_TARGET = "https://xboxapi.com/v2/%d/activity/recent";

    private final String xblApiKey; // App key to use API
    private final long xuid;      // XUID for TSD IRC account

    private TreeSet<Player> friendsList = new TreeSet<>(new Comparator<Player>() {
        @Override
        public int compare(Player o1, Player o2) {
            return o1.gamertag.compareToIgnoreCase(o2.gamertag);
        }
    });

    LoadingCache<Player, PlayerActivity> activityCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<Player, PlayerActivity>() {
                @Override
                public PlayerActivity load(Player player) throws Exception {
                    PlayerStatus status = fetchPlayerStatus(player);
                    if (status.state.equals(PlayerState.offline)) {
                        return new PlayerActivity(); // empty, offline
                    } else {
                        return fetchPlayerActivity(player);
                    }
                }
            });

    @Inject
    public XboxLive(TSDBot bot, Properties properties) throws IOException, URISyntaxException {
        super(bot);
        xblApiKey = properties.getProperty("xbl.apiKey");
        xuid = Long.parseLong(properties.getProperty("xbl.xuid"));
        loadFriendsList();
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length == 1) try {

            HashMap<String, List<String>> onlinePlayers = new HashMap<>(); // game -> players
            for(Player player : friendsList) {
                PlayerActivity activity = getCachedPlayerActivity(player);
                if (!activity.isOffline()) {
                    String game = activity.contentTitle;
                    if (!onlinePlayers.containsKey(game))
                        onlinePlayers.put(game, new LinkedList<String>());
                    onlinePlayers.get(game).add(player.gamertag);
                }
            }

            StringBuilder sb = new StringBuilder();
            boolean firstGame = true;
            for(String game : onlinePlayers.keySet()) {
                if(!firstGame) sb.append(" || ");
                sb.append(game).append(": ");
                sb.append(StringUtils.join(onlinePlayers.get(game), ", "));
                firstGame = false;
            }

            bot.sendMessage(channel, sb.toString());

        } catch (Exception e) {
            logger.error("Error getting XBL info", e);
            bot.sendMessage(channel, "Error getting XBL info");

        } else {

            String subCmd = cmdParts[1];

            if(subCmd.equals("reload")) {

                if(!bot.getUserFromNick(channel, sender).hasPriv(User.Priv.OP)) {
                    bot.sendMessage(channel, "Only ops can use .xbl reload");
                } else try {
                    loadFriendsList();
                    bot.sendMessage(channel, "Friends list reloaded successfully");
                } catch (Exception e) {
                    logger.error("Error reloading friends list", e);
                    bot.sendMessage(channel, "Error reloading friends list");
                }

            } else if(subCmd.equals("list")) {

                bot.sendMessage(channel, "Sending you my friends list, " + sender);
                StringBuilder sb = new StringBuilder();
                for(Player player : friendsList) {
                    if(sb.length() > 0) sb.append(", ");
                    sb.append(player.gamertag);
                }
                bot.sendMessage(sender, sb.toString());

            } else try {

                String gt = cmdParts[1];

                List<Player> matchedPlayers = FuzzyLogic.fuzzySubset(gt, friendsList, new FuzzyLogic.FuzzyVisitor<Player>() {
                    @Override
                    public String visit(Player o1) {
                        return o1.gamertag;
                    }
                });

                if(matchedPlayers.size() == 0) {
                    bot.sendMessage(channel, "Could not find gamertag matching \"" + gt + "\"... maybe I'm not following them?");
                } else if(matchedPlayers.size() > 1) {
                    StringBuilder msg = new StringBuilder();
                    msg.append("Found multiple gamertags matching \"").append(gt).append("\": ").append(StringUtils.join(matchedPlayers, ", "));
                    bot.sendMessage(channel, msg.toString());
                } else {
                    Player player = matchedPlayers.get(0);
                    PlayerActivity activity = getCachedPlayerActivity(player);
                    if(activity.isOffline()) {
                        bot.sendMessage(channel, player.gamertag + " is offline");
                    } else {
                        String fmt = "%s is online playing %s (%s)";
                        bot.sendMessage(channel, String.format(fmt, player.gamertag, activity.contentTitle, activity.platform.displayString));
                    }
                }

            } catch (Exception e) {
                logger.error("Error getting player status", e);
                bot.sendMessage(channel, "Error getting player status");
            }
        }
    }

    /**
     * Fetching functions
     */
    private String fetch(String target, Object... args) throws URISyntaxException, IOException {
        URIBuilder builder = new URIBuilder(String.format(target, args));
        URL url = new URL(builder.toString());
        URLConnection connection = url.openConnection();
        connection.addRequestProperty("X-AUTH", xblApiKey);
        String line;
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private PlayerStatus fetchPlayerStatus(Player player) throws IOException, URISyntaxException {
        String playerStatusJson = fetch(PRESENCE_TARGET, player.xuid);
        return new PlayerStatus(playerStatusJson);
    }

    private PlayerActivity getCachedPlayerActivity(Player player) throws IOException, URISyntaxException {
        try {
            return activityCache.get(player);
        } catch (ExecutionException e) {
            logger.error("Error getting cached activity for {}, defaulting to new fetch...", player.gamertag, e);
            return fetchPlayerActivity(player);
        }
    }

    private PlayerActivity fetchPlayerActivity(Player player) throws IOException, URISyntaxException {
        String playerActivityJson = fetch(ACTIVITY_TARGET, player.xuid);
        return new PlayerActivity(playerActivityJson);
    }

    private void loadFriendsList() throws IOException, URISyntaxException {
        friendsList.clear();
        String friendsJson = fetch(FRIENDS_TARGET, xuid);
        JSONArray list = new JSONArray(friendsJson);
        for (int i = 0; i < list.length(); i++) {
            JSONObject player = list.getJSONObject(i);
            Player p = new Player(player.getLong("id"), player.getString("Gamertag"));
            friendsList.add(p);
        }
    }

    /**
     * Wrapper classes
     */
    class Player {
        public long xuid;
        public String gamertag;

        Player(long xuid, String gamertag) {
            this.xuid = xuid;
            this.gamertag = gamertag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Player player = (Player) o;

            if (xuid != player.xuid) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return (int) (xuid ^ (xuid >>> 32));
        }
    }

    class PlayerStatus {
        public long xuid;
        public PlayerState state;
        public PlayerStatus(String json) {
            JSONObject playerStatus = new JSONObject(json);
            this.xuid = playerStatus.getLong("xuid");
            this.state = PlayerState.fromString(playerStatus.getString("state"));
        }
    }

    class PlayerActivity {
        public Platform platform;
        public String contentTitle;

        // used to represent an offline person
        public PlayerActivity() {}

        public PlayerActivity(String json) {
            JSONArray playerActivity = new JSONArray(json);
            JSONObject activity = playerActivity.getJSONObject(0);
            platform = Platform.fromFileString(activity.getString("platform"));
            contentTitle = activity.getString("contentTitle");
        }

        public boolean isOffline() {
            return contentTitle == null;
        }
    }

    enum PlayerState {
        online,
        offline;
        public static PlayerState fromString(String s) {
            for(PlayerState ps : values()) {
                if(s.equalsIgnoreCase(ps.toString()))
                    return ps;
            }
            return null;
        }
    }

    enum Platform {
        xbone("Xbone", "XboxOne"),
        x360("Xbox 360", "Xbox360");

        public String displayString;
        public String fileString;

        Platform(String displayString, String fileString) {
            this.displayString = displayString;
            this.fileString = fileString;
        }

        public static Platform fromFileString(String fs) {
            for(Platform p : values()) {
                if(p.fileString.equals(fs))
                    return p;
            }
            return null;
        }
    }

}
