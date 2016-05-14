package org.tsd.tsdbot.util;

import com.google.inject.Inject;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.jibble.pircbot.User;
import org.tsd.tsdbot.functions.Archivist;
import org.tsd.tsdbot.functions.MainFunction;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.runnable.IRCListenerThread;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.stats.Stats;

import java.util.Set;

public class MessageHandler {

    protected static Set<User> blacklist = new ConcurrentHashSet<>();

    @Inject
    protected Archivist archivist;

    @Inject
    protected Set<MainFunction> functions;

    @Inject
    protected ThreadManager threadManager;

    @Inject
    protected Set<Stats> stats;

    @Inject
    protected HistoryBuff historyBuff;

    public Set<User> getBlacklist() {
        return blacklist;
    }

    public void handleMessage(User user, String channel, String sender, String login, String hostname, String message) {
        archivist.log(channel, ArchivistUtil.getRawMessage(
                System.currentTimeMillis(),
                sender,
                login,
                message
        ));

        if(!blacklist.contains(user)) {
            // pass message to functions that match its pattern
            for (MainFunction function : functions) {
                if (message.matches(function.getListeningRegex())) {
                    function.run(channel, sender, login, message);
                }
            }

            // propagate message to all listening threads
            for (IRCListenerThread listenerThread : threadManager.getThreadsByChannel(channel)) {
                if (listenerThread.matches(message)) {
                    listenerThread.onMessage(sender, login, hostname, message);
                }
            }
        }

        // pass message to all stat-collecting entities
        for(Stats s : stats) {
            s.processMessage(channel, sender, login, hostname, message);
        }

        historyBuff.updateHistory(channel, message, sender);
    }
}
