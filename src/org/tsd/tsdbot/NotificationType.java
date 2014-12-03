package org.tsd.tsdbot;

import org.tsd.tsdbot.notifications.*;

import java.util.LinkedList;
import java.util.List;

/**
* Created by Joe on 11/18/2014.
*/
public enum NotificationType {
    HBO_FORUM("HBO Forum",  Command.HBO_FORUM,  HboForumManager.class),
    HBO_NEWS("HBO News",    Command.HBO_NEWS,   HboNewsManager.class),
    DBO_FORUM("DBO Forum",  Command.DBO_FORUM,  DboForumManager.class),
    DBO_NEWS("DBO News",    Command.DBO_NEWS,   DboNewsManager.class),
    TWITTER("Twitter",      Command.TWITTER,    TwitterManager.class);

    private String displayString;
    private Command accessCommand;
    private Class<? extends NotificationManager> managerMap;

    NotificationType(String displayString, Command accessCommand, Class<? extends NotificationManager> managerMap) {
        this.displayString = displayString;
        this.accessCommand = accessCommand;
        this.managerMap = managerMap;
    }

    public String getDisplayString() {
        return displayString;
    }

    public Command getAccessCommand() {
        return accessCommand;
    }

    public Class<? extends NotificationManager> getManagerMap() {
        return managerMap;
    }

    public static NotificationType fromCommand(Command cmd) {
        for(NotificationType type : values()) {
            if(type.accessCommand.equals(cmd)) return type;
        }
        return null;
    }

    public static List<NotificationType> fromManager(NotificationManager manager) {
        LinkedList<NotificationType> matches = new LinkedList<>();
        for(NotificationType type : values()) {
            if(manager.getClass().equals(type.getManagerMap()))
                matches.add(type);
        }
        return matches;
    }
}
