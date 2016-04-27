package org.tsd.tsdbot.history.filter;

import org.tsd.tsdbot.history.HistoryBuff;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NoBotsStrategy implements MessageFilterStrategy {

    private static final Set<String> knownBots = new HashSet<>(
            Arrays.asList("pybot", "bonk-bot")
    );

    @Override
    public boolean apply(HistoryBuff.Message m) {
        return (!knownBots.contains(m.sender.toLowerCase())) && !m.sender.toLowerCase().contains("bot");
    }
}
