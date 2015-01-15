package org.tsd.tsdbot.runnable;

import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.ThreadType;

import java.util.concurrent.Callable;

/**
 * Created by Joe on 2/22/14.
 */
public abstract class IRCListenerThread implements Callable {

    protected ThreadManager manager;
    protected TSDBot bot;
    protected String channel;
    protected String listeningRegex;
    protected long startTime = -1;
    protected final Object mutex = new Object();

    public IRCListenerThread(TSDBot bot, ThreadManager threadManager) {
        this.bot = bot;
        this.manager = threadManager;
    }

    public String getChannel() {
        return channel;
    }

    public abstract ThreadType getThreadType();
    public abstract void onMessage(String sender, String login, String hostname, String message);
    public abstract void onPrivateMessage(String sender, String login, String hostname, String message);
    public abstract long getRemainingTime();

    public boolean matches(String text) {
        return text.matches(listeningRegex);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        else if(o instanceof IRCListenerThread) {
            IRCListenerThread that = (IRCListenerThread)o;
            return channel.equals(that.channel)
                    && getThreadType().equals(that.getThreadType());
        } else return false;
    }

    @Override
    public int hashCode() {
        return (getThreadType() + channel).hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
