package org.tsd.tsdbot.runnable;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.tsd.tsdbot.TSDBot;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Joe on 2/22/14.
 */
public class ThreadManager {

    private ExecutorService threadPool;

    // row: type
    // col: channel
    // value: thread
    private Table<TSDBot.ThreadType,String,IRCListenerThread> runningIrcThreads;

    public ThreadManager(int poolSize) {
        threadPool = Executors.newFixedThreadPool(poolSize);
        runningIrcThreads = HashBasedTable.create(TSDBot.ThreadType.values().length, 10);
    }

    public synchronized void addThread(IRCListenerThread thread) throws DuplicateThreadException {

        if(runningIrcThreads.contains(thread.getThreadType(), thread.getChannel()))
            throw new DuplicateThreadException(
                    "Duplicate threads: " + thread.getThreadType() + " / " + thread.getChannel()
            );

        threadPool.submit(thread);
        runningIrcThreads.put(thread.getThreadType(), thread.getChannel(), thread);
    }

    public synchronized void removeThread(IRCListenerThread thread) {
        runningIrcThreads.remove(thread.getThreadType(), thread.getChannel());
    }

    public IRCListenerThread getIrcThread(TSDBot.ThreadType threadType, String channel) {
        return runningIrcThreads.get(threadType,channel);
    }

    public Collection<IRCListenerThread> getAllIrcThreads() {
        return runningIrcThreads.values();
    }

    public Collection<IRCListenerThread> getThreadsByType(TSDBot.ThreadType threadType) {
        Map<String,IRCListenerThread> map = runningIrcThreads.row(threadType);
        if(map != null)
            return map.values();
        else
            return new LinkedList<>();
    }

    public Collection<IRCListenerThread> getThreadsByChannel(String channel) {
        Map<TSDBot.ThreadType,IRCListenerThread> map = runningIrcThreads.column(channel);
        if(map != null)
            return map.values();
        else
            return new LinkedList<>();
    }

}
