package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.history.MessageFilter;
import org.tsd.tsdbot.history.MessageFilterStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

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

    @Inject
    public XboxLive(TSDBot bot, Properties properties) {
        super(bot);
        xblApiKey = properties.getProperty("xbl.apiKey");
        xuid = Long.parseLong(properties.getProperty("xbl.xuid"));
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        try {
            HashMap<String, List<String>> onlinePlayers = new HashMap<>(); // game -> players
            String friendsJson = fetch(FRIENDS_TARGET, xuid);
            JSONArray friendsList = new JSONArray(friendsJson);
            for(int i=0 ; i < friendsList.length() ; i++) {
                JSONObject player = friendsList.getJSONObject(i);
                long playerXuid = player.getLong("id");
                String playerStatusJson = fetch(PRESENCE_TARGET, playerXuid);
                JSONObject playerStatus = new JSONObject(playerStatusJson);
                if(PlayerState.fromString(playerStatus.getString("state")).equals(PlayerState.online)) {
                    String playerActivityJson = fetch(ACTIVITY_TARGET, playerXuid);
                    JSONArray playerActivity = new JSONArray(playerActivityJson);
                    JSONObject activity = playerActivity.getJSONObject(0);
                    String game = activity.getString("contentTitle");
                    if(!onlinePlayers.containsKey(game))
                        onlinePlayers.put(game, new LinkedList<String>());
                    onlinePlayers.get(game).add(player.getString("Gamertag"));
                }
            }

            StringBuilder sb = new StringBuilder();
            boolean firstGame = true;
            for(String game : onlinePlayers.keySet()) {
                if(!firstGame) sb.append(" || ");
                sb.append(game).append(": ");
                sb.append(StringUtils.join(onlinePlayers.get(game), ","));
                firstGame = false;
            }

            bot.sendMessage(channel, sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

}
