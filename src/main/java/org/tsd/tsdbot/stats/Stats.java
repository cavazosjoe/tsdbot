package org.tsd.tsdbot.stats;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by Joe on 1/11/2015.
 */
public interface Stats {
    public LinkedHashMap<String, Object> getReport();
    public void processMessage(String channel, String sender, String login, String hostname, String message);
    public void processAction(String sender, String login, String hostname, String target, String action);
}
