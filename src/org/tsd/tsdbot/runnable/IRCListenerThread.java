package org.tsd.tsdbot.runnable;

import org.tsd.tsdbot.Command;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.ThreadType;

import java.util.HashSet;
import java.util.concurrent.Callable;

/**
 * Created by Joe on 2/22/14.
 */
public abstract class IRCListenerThread implements Callable {

    protected ThreadManager manager;
    protected TSDBot bot;
    protected String channel;
    protected HashSet<Command> listeningCommands;
    protected long startTime = -1;
    protected final Object mutex = new Object();

    public IRCListenerThread(String channel) {
        this.channel = channel;
        this.listeningCommands = new HashSet<>();
    }

    public String getChannel() {
        return channel;
    }

    public abstract ThreadType getThreadType();
    public abstract void onMessage(Command command, String sender, String login, String hostname, String message);
    public abstract void onPrivateMessage(Command command, String sender, String login, String hostname, String message);
    public abstract long getRemainingTime();

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
}
