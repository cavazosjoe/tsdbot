package org.tsd.tsdbot.runnable;

import org.tsd.tsdbot.TSDBot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Joe on 2/22/14.
 */
public class ThreadManager {

    private ExecutorService threadPool;

    // STRAWPOLL -> [ poll1 in #chan1, poll2 in #chan2, poll3 in #chan3 ]
    private HashMap<TSDBot.ThreadType,HashSet<IRCListenerThread>> runningIrcThreads;

    public ThreadManager(int poolSize) {
        threadPool = Executors.newFixedThreadPool(poolSize);

        for(TSDBot.ThreadType type : TSDBot.ThreadType.values())
            runningIrcThreads.put(type, new HashSet<IRCListenerThread>());
    }

    public void addThread(TSDBot.ThreadType threadType, IRCListenerThread thread) {
        threadPool.submit(thread);

        if()
        runningIrcThreads.put(threadType,thread);
    }

    public void removeThread(IRCListenerThread thread) {
        runningIrcThreads.remove(thread);
    }

    public HashSet<IRCListenerThread> getRunningThreads() {
        return runningIrcThreads;
    }

    public void propagateMsg(String channel, String sender, String login, String hostname, String message) {
        for(IRCListenerThread thread : runningIrcThreads)
            thread.onMessage(channel, sender, login, hostname, message);
    }

    public void propagatePrvMsg(String sender, String login, String hostname, String message) {
        for(IRCListenerThread thread : runningIrcThreads)
            thread.onPrivateMessage(sender, login, hostname, message);
    }
}
