package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.http.client.HttpClient;

import javax.naming.OperationNotSupportedException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Joe on 2/18/14.
 */
public abstract class NotificationManager {
    public abstract LinkedList<? extends NotificationEntity> sweep(HttpClient client) throws OperationNotSupportedException;
    public abstract LinkedList<? extends NotificationEntity> sweep(WebClient webClient) throws OperationNotSupportedException;
    public abstract List<? extends NotificationEntity> history();
    public abstract NotificationEntity expand(String key);
}
