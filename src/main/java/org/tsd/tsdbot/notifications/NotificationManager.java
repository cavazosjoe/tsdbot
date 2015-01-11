package org.tsd.tsdbot.notifications;

import java.util.LinkedList;

/**
 * Created by Joe on 2/18/14.
 */
public abstract class NotificationManager<T extends NotificationEntity> {

    protected int MAX_HISTORY;
    protected LinkedList<T> recentNotifications = new LinkedList<>();

    public NotificationManager(int maxHistory) {
        this.MAX_HISTORY = maxHistory;
    }

    public abstract LinkedList<T> sweep();

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

}
