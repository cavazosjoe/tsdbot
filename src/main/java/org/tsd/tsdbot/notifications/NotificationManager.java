package org.tsd.tsdbot.notifications;

import org.tsd.tsdbot.NotificationType;
import org.tsd.tsdbot.TSDBot;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class NotificationManager<T extends NotificationEntity> {

    protected TSDBot bot;
    protected int MAX_HISTORY;
    protected LinkedList<T> recentNotifications = new LinkedList<>();
    protected List<String> channels;
    private boolean muted;

    public NotificationManager(TSDBot bot, int maxHistory, boolean muted) {
        this.bot = bot;
        this.MAX_HISTORY = maxHistory;
        this.muted = muted;
    }

    public abstract NotificationType getNotificationType();
    protected abstract List<T> sweep();

    public List<T> history() {
        return recentNotifications;
    }

    public List<T> getNotificationByTail(String q) {
        return recentNotifications.stream()
                .filter(notification -> q.equals(notification.getKey()) || notification.getKey().endsWith(q))
                .collect(Collectors.toList());
    }

    public T getNotificationExact(String key) {
        return recentNotifications.stream()
                .filter(notification -> key.equals(notification.getKey()))
                .findFirst().orElse(null);
    }

    protected void trimHistory() {
        while(recentNotifications.size() > MAX_HISTORY) {
            recentNotifications.removeLast();
        }
    }

    public void sweepAndNotify() {
        if(!muted) {
            sweep().stream()
                    .map(NotificationEntity::getInline)
                    .forEach(notification -> {
                        channels.stream()
                                .forEach(channel -> bot.sendMessage(channel, notification));
                    });
        }
        muted = false;
    }

}
