package org.tsd.tsdbot;

import java.util.LinkedList;

/**
 * Created by Joe on 2/18/14.
 */
public abstract class NotificationManager {
    public abstract LinkedList<? extends NotificationEntity> sweep();
    public abstract LinkedList<? extends NotificationEntity> history();
    public abstract NotificationOrigin getOrigin();

    public enum NotificationOrigin {
        HBO_FORUM("HBO Forum"),
        HBO_NEWS("HBO News"),
        DBO_FORUM("DBO Forum"),
        DBO_NEWS("DBO News"),
        TWITTER("Twitter");

        private String displayString;

        NotificationOrigin(String displayString) {
            this.displayString = displayString;
        }

        public String getDisplayString() {
            return displayString;
        }
    }
}
