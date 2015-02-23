package org.tsd.tsdbot.notifications;

import org.tsd.tsdbot.NotificationType;
import org.tsd.tsdbot.TSDBot;

import java.util.LinkedList;

/**
 * Created by Joe on 2/18/14.
 */
public abstract class NotificationManager<T extends NotificationEntity> {

    protected TSDBot bot;
    protected int MAX_HISTORY;
    protected LinkedList<T> recentNotifications = new LinkedList<>();
    protected String[] channels;
    private boolean muted;

    public NotificationManager(TSDBot bot, int maxHistory, boolean muted) {
        this.bot = bot;
        this.MAX_HISTORY = maxHistory;
        this.muted = muted;
    }

    public abstract NotificationType getNotificationType();
    protected abstract LinkedList<T> sweep();

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

    public void sweepAndNotify() {
        for(NotificationEntity notification : sweep()) {
            if(!muted) {
                for(String channel : channels) {
                    bot.sendMessage(channel, notification.getInline());
                }
            }
        }
        muted = false;
    }

}
