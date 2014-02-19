package org.tsd.tsdbot;

import org.apache.http.client.HttpClient;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by Joe on 2/18/14.
 */
public abstract class NotificationManager {
    public abstract LinkedList<? extends NotificationEntity> sweep(HttpClient client);
    public abstract List<? extends NotificationEntity> history();
    public abstract NotificationEntity expand(String key);
}
