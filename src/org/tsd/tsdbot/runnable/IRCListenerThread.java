package org.tsd.tsdbot.runnable;

import java.util.concurrent.Callable;

/**
 * Created by Joe on 2/22/14.
 */
public abstract class IRCListenerThread implements Callable {

    private ThreadManager manager;
    protected String channel;

    public IRCListenerThread(ThreadManager threadManager, String channel) {
        this.manager = threadManager;
    }

    protected void shutdown() {
        manager.removeThread(this);
    }

    public String getChannel() {
        return channel;
    }

    public abstract void onMessage(String channel, String sender, String login, String hostname, String message);
    public abstract void onPrivateMessage(String sender, String login, String hostname, String message);

    @Override
    public int hashCode() {
        return channel.hashCode();
    }
}
