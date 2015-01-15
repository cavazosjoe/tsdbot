package org.tsd.tsdbot;

import org.tsd.tsdbot.notifications.*;
import org.tsd.tsdbot.rss.RssFeedManager;

/**
 * Created by Joe on 1/13/2015.
 */
public enum NotificationType {
    HBO_FORUM("HBO Forum", ".hbof", HboForumManager.class),
    HBO_NEWS("HBO News",   ".hbon", HboNewsManager.class),
    DBO_FORUM("DBO Forum", ".dbof", DboForumManager.class),
    DBO_NEWS("DBO News",   ".dbon", DboNewsManager.class),
    TWITTER("Twitter",     ".tw",   TwitterManager.class),
    RSS_ITEM("RSS",        ".rss", RssFeedManager.class);

    private String displayString;
    private String commandPrefix;
    private Class<? extends NotificationManager> managerMap;

    NotificationType(String displayString, String commandPrefix, Class<? extends NotificationManager> managerMap) {
        this.displayString = displayString;
        this.managerMap = managerMap;
        this.commandPrefix = commandPrefix;
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public String getDisplayString() {
        return displayString;
    }

    public Class<? extends NotificationManager> getManagerMap() {
        return managerMap;
    }

    public static NotificationType fromCommand(String cmd) {
        for(NotificationType type : values()) {
            if(cmd.startsWith(type.getCommandPrefix()))
                return type;
        }
        return null;
    }
}
