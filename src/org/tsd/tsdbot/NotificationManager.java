package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.http.client.HttpClient;

import javax.naming.OperationNotSupportedException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Joe on 2/18/14.
 */
public abstract class NotificationManager<T extends NotificationEntity> {
    public abstract LinkedList<T> sweep(HttpClient client) throws OperationNotSupportedException;
    public abstract LinkedList<T> sweep(WebClient webClient) throws OperationNotSupportedException;
    public abstract LinkedList<T> history();
    public abstract T expand(String key);
}
