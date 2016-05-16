package org.tsd.tsdbot.stats;

import java.util.LinkedHashMap;

public interface Stats {
    LinkedHashMap<String, Object> getReport();
    void processMessage(String channel, String sender, String login, String hostname, String message);
    void processAction(String sender, String login, String hostname, String target, String action);
}
