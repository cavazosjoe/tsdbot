package org.tsd.tsdbot.config;

import org.tsd.tsdbot.Stage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConnectionConfig {

    public String ident;
    public String nick;
    public String nickservPass;
    public String server;
    public String mainChannel;
    public List<String> auxChannels;
    public Map<String, List<String>> notifiers;
    public Stage stage;

    public List<String> getAllChannels() {
        List<String> allChannels = new LinkedList<>();
        allChannels.addAll(auxChannels);
        allChannels.add(mainChannel);
        return allChannels;
    }

}
