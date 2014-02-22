package org.tsd.tsdbot;

import javax.naming.OperationNotSupportedException;
import java.util.LinkedList;

/**
 * Created by Joe on 2/18/14.
 */
public abstract class NotificationManager<T extends NotificationEntity> {

    protected int MAX_HISTORY = 0;
    protected LinkedList<T> recentNotifications = new LinkedList<>();

    public NotificationManager(int maxHistory) {
        this.MAX_HISTORY = maxHistory;
    }

    public abstract LinkedList<T> sweep();
    public abstract NotificationOrigin getOrigin();

    public LinkedList<T> history() {
        return recentNotifications;
    }

    public LinkedList<T> getNotificationByTail(String q) {
        LinkedList<T> matchedNotifications = new LinkedList<>();
        for(T notification : recentNotifications) {
            if(q.equals(notification.getKey()) || notification.getKey().endsWith(q))
                matchedNotifications.add(notification);
        }
        return matchedNotifications;
    }

    public T getNotificationExact(String key) {
        for(T notification : recentNotifications) {
            if(key.equals(notification.getKey())) return notification;
        }
        return null;
    }

    protected void trimHistory() {
        while(recentNotifications.size() > MAX_HISTORY) recentNotifications.removeLast();
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
