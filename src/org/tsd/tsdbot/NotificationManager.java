package org.tsd.tsdbot;

import java.util.LinkedList;

/**
 * Created by Joe on 2/18/14.
 */
public abstract class NotificationManager<T extends NotificationEntity> {
    public abstract LinkedList<T> sweep();
    public abstract LinkedList<T> history();
    public abstract NotificationOrigin getOrigin();

    public LinkedList<NotificationEntity> getNotificationByTail(String q) {
        LinkedList<NotificationEntity> matchedNotifications = new LinkedList<>();
        for(NotificationEntity notification : history()) {
            if(q.equals(notification.getKey()) || notification.getKey().endsWith(q))
                matchedNotifications.add(notification);
        }
        return matchedNotifications;
    }

    public NotificationEntity getNotificationExact(String key) {
        for(NotificationEntity notification : history()) {
            if(key.equals(notification.getKey())) return notification;
        }
        return null;
    }

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
