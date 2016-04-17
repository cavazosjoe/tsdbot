package org.tsd.tsdbot.config;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;
import org.tsd.tsdbot.Stage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConnectionConfig {

    @NotEmpty
    @NotNull
    public String ident;

    @NotEmpty
    @NotNull
    public String nick;

    public String nickservPass;

    @NotEmpty
    @NotNull
    public String server;

    @NotEmpty
    @NotNull
    public Integer port = 6667;

    @NotEmpty
    @NotNull
    public String mainChannel;

    @NotNull
    public List<String> auxChannels;

    @NotNull
    public List<String> tsdtvChannels;

    @NotNull
    public List<String> tsdfmChannels;

    public Map<String, List<String>> notifiers;

    @NotEmpty
    @NotNull
    public Stage stage;

    public List<String> getAllChannels() {
        List<String> allChannels = new LinkedList<>();
        if(auxChannels != null) {
            allChannels.addAll(auxChannels);
        }
        allChannels.add(mainChannel);
        return allChannels;
    }
}
