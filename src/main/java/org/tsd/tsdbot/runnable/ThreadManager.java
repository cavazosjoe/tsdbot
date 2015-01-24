package org.tsd.tsdbot.runnable;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.ThreadType;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Joe on 2/22/14.
 */
public class ThreadManager {

    private static final Logger logger = LoggerFactory.getLogger(ThreadManager.class);

    private ExecutorService threadPool;

    // row: type
    // col: channel
    // value: thread
    private Table<ThreadType,String,IRCListenerThread> runningIrcThreads;

    public ThreadManager(int poolSize) {
        threadPool = Executors.newFixedThreadPool(poolSize);
        runningIrcThreads = HashBasedTable.create(ThreadType.values().length, 10);
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
        logger.info("Removing thread: {}", thread.toString());
        runningIrcThreads.remove(thread.getThreadType(), thread.getChannel());
    }

    public IRCListenerThread getIrcThread(ThreadType threadType, String channel) {
        return runningIrcThreads.get(threadType,channel);
    }

    public Collection<IRCListenerThread> getAllIrcThreads() {
        return runningIrcThreads.values();
    }

    public Collection<IRCListenerThread> getThreadsByType(ThreadType threadType) {
        Map<String,IRCListenerThread> map = runningIrcThreads.row(threadType);
        if(map != null)
            return map.values();
        else
            return new LinkedList<>();
    }

    public Collection<IRCListenerThread> getThreadsByChannel(String channel) {
        Map<ThreadType,IRCListenerThread> map = runningIrcThreads.column(channel);
        if(map != null)
            return map.values();
        else
            return new LinkedList<>();
    }

}
