package org.tsd.tsdbot;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.functions.Archivist;
import org.tsd.tsdbot.functions.Hustle;
import org.tsd.tsdbot.functions.MainFunction;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.notifications.NotificationManager;
import org.tsd.tsdbot.runnable.IRCListenerThread;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.stats.Stats;
import org.tsd.tsdbot.util.ArchivistUtil;
import org.tsd.tsdbot.util.FuzzyLogic;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBot extends PircBot {

    private static Logger logger = LoggerFactory.getLogger(TSDBot.class);

    public static long blunderCount = 0;
    public static boolean showNotifications = false;

    @Inject
    private ThreadManager threadManager;

    @Inject
    private Set<MainFunction> functions;

    @Inject
    private Set<Stats> stats;

    @Inject
    protected HistoryBuff historyBuff;

    @Inject
    protected Archivist archivist;

    @Inject
    protected Hustle hustle;

    public TSDBot(String ident, String name, String nickservPass, String server, String[] channels) throws IrcException, IOException {
        setName(name);
        setAutoNickChange(true);
        setLogin(ident);
        setVerbose(false);
        setMessageDelay(10); //10 ms
        connect(server);
        if(!StringUtils.isEmpty(nickservPass))
            identify(nickservPass);

        for(String channel : channels) {
            joinChannel("#"+channel);
            logger.info("Joined channel {}", channel);
        }
    }

    @Override
    public synchronized void sendMessage(String target, String text) {
        super.sendMessage(target, text);
        archivist.log(target, ArchivistUtil.getRawMessage(
                System.currentTimeMillis(),
                getNick(),
                getLogin(),
                text
        ));
    }

    @Override protected synchronized void onUserList(String channel, User[] users) {}
    @Override protected synchronized void onPrivateMessage(String sender, String login, String hostname, String message) {}
    @Override protected synchronized void onAction(String sender, String login, String hostname, String target, String action) {
        archivist.log(target, ArchivistUtil.getRawAction(
                System.currentTimeMillis(),
                sender,
                login,
                action
        ));
    }
    @Override protected synchronized void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {}
    @Override protected synchronized void onJoin(String channel, String sender, String login, String hostname) {
        archivist.log(channel, ArchivistUtil.getRawJoin(
                System.currentTimeMillis(),
                sender,
                login,
                hostname,
                channel
        ));
    }
    @Override protected synchronized void onPart(String channel, String sender, String login, String hostname) {
        // when someone leaves the channel
        archivist.log(channel, ArchivistUtil.getRawPart(
                System.currentTimeMillis(),
                sender,
                login,
                channel
        ));
    }
    @Override protected synchronized void onNickChange(String oldNick, String login, String hostname, String newNick) {
        archivist.log(null, ArchivistUtil.getRawNickChange(
                System.currentTimeMillis(),
                oldNick,
                newNick
        ));
    }
    @Override protected synchronized void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        archivist.log(channel, ArchivistUtil.getRawKick(
                System.currentTimeMillis(),
                kickerNick,
                recipientNick,
                channel,
                reason
        ));
    }
    @Override protected synchronized void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
        // when someone quits the server
        archivist.log(null, ArchivistUtil.getRawQuit(
                System.currentTimeMillis(),
                sourceNick,
                sourceLogin,
                reason
        ));
    }
    @Override protected synchronized void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
        archivist.log(channel, ArchivistUtil.getRawTopicChange(
                System.currentTimeMillis(),
                setBy,
                topic
        ));
    }
    @Override protected synchronized void onChannelInfo(String channel, int userCount, String topic) {}
    @Override protected synchronized void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
        archivist.log(channel, ArchivistUtil.getRawChannelMode(
                System.currentTimeMillis(),
                sourceNick,
                mode,
                channel
        ));
    }
    @Override protected synchronized void onUserMode(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
        archivist.log(null, ArchivistUtil.getRawUserMode(
                System.currentTimeMillis(),
                sourceNick,
                mode,
                targetNick
        ));
    }

    @Override
    protected synchronized void onMessage(String channel, String sender, String login, String hostname, String message) {

        logger.info("{}: <{}> {}", new Object[]{channel, sender, message});

        archivist.log(channel, ArchivistUtil.getRawMessage(
                System.currentTimeMillis(),
                sender,
                login,
                message
        ));

        // pass message to functions that match its pattern
        for(MainFunction function : functions) {
            if(message.matches(function.getRegex())) {
                function.engage(channel, sender, login, message);
            }
        }

        // propagate message to all listening threads
        for(IRCListenerThread listenerThread : threadManager.getThreadsByChannel(channel)) {
            if(listenerThread.matches(message)) {
                listenerThread.onMessage(sender, login, hostname, message);
            }
        }

        // pass message to all stat-collecting entities
        for(Stats s : stats) {
            s.processMessage(channel, sender, login, hostname, message);
        }

        historyBuff.updateHistory(channel, message, sender);

    }

    public LinkedList<User> getNonBotUsers(String channel) {
        LinkedList<User> ret = new LinkedList<>();
        for(User u : getUsers(channel)) {
            if( (!FuzzyLogic.fuzzyMatches("bot", u.getNick())) && (!u.getNick().equalsIgnoreCase("tipsfedora")) )
                ret.add(u);
        }
        return ret;
    }

    public User getUserFromNick(String channel, String nick) {
        User user = null;
        String prefixlessNick;
        for(User u : getUsers(channel)) {
            prefixlessNick = u.getNick().replaceAll(u.getPrefix(),"");
            if(prefixlessNick.equals(nick)) {
                user = u;
                break;
            }
        }
        return user;
    }

    public void sendMessages(String target, String[] messages) {
        for(String m : messages) sendMessage(target, m);
    }

    public void broadcast(String message) {
        for(String channel : getChannels()) {
            sendMessage(channel, message);
        }
    }
}
