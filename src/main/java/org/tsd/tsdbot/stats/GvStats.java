package org.tsd.tsdbot.stats;

import com.google.inject.Inject;
import org.tsd.tsdbot.TSDBot;

import java.util.Arrays;
import java.util.LinkedHashMap;

public class GvStats implements Stats {

    @Inject
    private TSDBot bot;

    @Override
    public LinkedHashMap<String, Object> getReport() {
        LinkedHashMap<String, Object> report = new LinkedHashMap<>();

        report.put("GV Status",
                (isGvOnline()) ? "<span style='color: red'>ONLINE</span>" : "offline");

        return report;
    }

    @Override
    public void processMessage(String channel, String sender, String login, String hostname, String message) {

    }

    @Override
    public void processAction(String sender, String login, String hostname, String target, String action) {

    }

    private boolean isGvOnline() {
        return Arrays.stream(bot.getChannels())
                .flatMap(channel -> Arrays.stream(bot.getUsers(channel)))
                .map(user -> user.getNick().toLowerCase())
                .anyMatch(nick -> nick.contains("general") || nick.contains("gv"));
    }
}
