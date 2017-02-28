package org.tsd.tsdbot;

public enum NotificationType {
    HBO_FORUM("HBO Forum", ".hbof"),
    HBO_NEWS("HBO News",   ".hbon"),
    DBO_FORUM("DBO Forum", ".dbof"),
    DBO_NEWS("DBO News",   ".dbon"),
    TWITTER("Twitter",     ".tw"),
    RSS_ITEM("RSS",        ".rss");

    private String displayString;
    private String commandPrefix;

    NotificationType(String displayString, String commandPrefix) {
        this.displayString = displayString;
        this.commandPrefix = commandPrefix;
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public String getDisplayString() {
        return displayString;
    }

    public static NotificationType fromCommand(String cmd) {
        for(NotificationType type : values()) {
            if(cmd.startsWith(type.getCommandPrefix())) {
                return type;
            }
        }
        return null;
    }
}
