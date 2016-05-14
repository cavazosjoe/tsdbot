package org.tsd.tsdbot;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.functions.Archivist;
import org.tsd.tsdbot.functions.Hustle;
import org.tsd.tsdbot.functions.MainFunction;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.module.BotOwner;
import org.tsd.tsdbot.module.MainChannel;
import org.tsd.tsdbot.runnable.IRCListenerThread;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.stats.Stats;
import org.tsd.tsdbot.util.ArchivistUtil;
import org.tsd.tsdbot.util.IRCUtil;
import org.tsd.tsdbot.util.fuzzy.FuzzyLogic;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;

public class TSDBot extends PircBot implements Bot {

    private static Logger logger = LoggerFactory.getLogger(TSDBot.class);

    protected static long blunderCount = 0;
    protected static Set<User> blacklist = new ConcurrentHashSet<>();

    @Inject
    protected ThreadManager threadManager;

    @Inject
    protected Set<MainFunction> functions;

    @Inject
    protected Set<Stats> stats;

    @Inject
    protected HistoryBuff historyBuff;

    @Inject
    protected Archivist archivist;

    @Inject
    protected Hustle hustle;

    @Inject @MainChannel
    protected String mainChannel;

    @Inject @BotOwner
    protected String owner;

    @Inject @Named("loggingProperties")
    protected File loggingPropertiesFile;

    @Deprecated // used for testing
    public TSDBot() {}

    public TSDBot(String ident, String name, String nickservPass, String server, int port) throws IrcException, IOException {
        setName(name);
        setAutoNickChange(true);
        setLogin(ident);
        setVerbose(false);
        setMessageDelay(10); //10 ms
        connect(server, port);
        logger.info("Successfully connected to " + server + ":" + port);
        if(StringUtils.isNotBlank(nickservPass)) {
            identify(nickservPass);
        }
    }

    public long getBlunderCount() {
        return blunderCount;
    }

    @Override
    public synchronized void sendMessage(String target, String text) {
        super.sendMessage(target, text);
        if(archivist != null) {
            archivist.log(target, ArchivistUtil.getRawMessage(
                    System.currentTimeMillis(),
                    getNick(),
                    getLogin(),
                    text
            ));
        }
    }

    @Override public synchronized void onUserList(String channel, User[] users) {}
    @Override public synchronized void onPrivateMessage(String sender, String login, String hostname, String message) {}
    @Override public synchronized void onAction(String sender, String login, String hostname, String target, String action) {
        archivist.log(target, ArchivistUtil.getRawAction(
                System.currentTimeMillis(),
                sender,
                login,
                action
        ));
    }
    @Override public synchronized void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {}
    @Override public synchronized void onJoin(String channel, String sender, String login, String hostname) {
        archivist.log(channel, ArchivistUtil.getRawJoin(
                System.currentTimeMillis(),
                sender,
                login,
                hostname,
                channel
        ));
    }
    @Override public synchronized void onPart(String channel, String sender, String login, String hostname) {
        // when someone leaves the channel
        archivist.log(channel, ArchivistUtil.getRawPart(
                System.currentTimeMillis(),
                sender,
                login,
                channel
        ));
    }
    @Override public synchronized void onNickChange(String oldNick, String login, String hostname, String newNick) {
        archivist.log(null, ArchivistUtil.getRawNickChange(
                System.currentTimeMillis(),
                oldNick,
                newNick
        ));
    }
    @Override public synchronized void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        archivist.log(channel, ArchivistUtil.getRawKick(
                System.currentTimeMillis(),
                kickerNick,
                recipientNick,
                channel,
                reason
        ));
    }
    @Override public synchronized void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
        // when someone quits the server
        archivist.log(null, ArchivistUtil.getRawQuit(
                System.currentTimeMillis(),
                sourceNick,
                sourceLogin,
                reason
        ));
    }
    @Override public synchronized void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
        archivist.log(channel, ArchivistUtil.getRawTopicChange(
                System.currentTimeMillis(),
                setBy,
                topic
        ));
    }
    @Override public synchronized void onChannelInfo(String channel, int userCount, String topic) {}
    @Override public synchronized void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
        archivist.log(channel, ArchivistUtil.getRawChannelMode(
                System.currentTimeMillis(),
                sourceNick,
                mode,
                channel
        ));
    }
    @Override public synchronized void onUserMode(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
        archivist.log(null, ArchivistUtil.getRawUserMode(
                System.currentTimeMillis(),
                sourceNick,
                mode,
                targetNick
        ));
    }

    @Override
    public synchronized void onMessage(String channel, String sender, String login, String hostname, String message) {

        logger.info("{}: <{}> {}", new Object[]{channel, sender, message});

        archivist.log(channel, ArchivistUtil.getRawMessage(
                System.currentTimeMillis(),
                sender,
                login,
                message
        ));

        if(!blacklist.contains(getUserFromNick(channel, sender))) {
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

    @Override
    public LinkedList<User> getNonBotUsers(String channel) {
        LinkedList<User> ret = new LinkedList<>();
        for(User u : getUsers(channel)) {
            if( (!FuzzyLogic.fuzzyMatches("bot", u.getNick())) && (!FuzzyLogic.fuzzyMatches("wheatley", u.getNick())) ) {
                ret.add(u);
            }
        }
        return ret;
    }

    @Override
    public boolean addToBlacklist(User user) {
        return blacklist.add(user);
    }

    @Override
    public boolean removeFromBlacklist(User user) {
        return blacklist.remove(user);
    }

    @Override
    public boolean userIsOwner(String nick) {
        return nick.equals(owner);
    }

    @Override
    public boolean userHasGlobalPriv(String nick, User.Priv priv) {
        return userHasPrivInChannel(nick, mainChannel, priv);
    }

    @Override
    public boolean userHasPrivInChannel(String nick, String channel, User.Priv priv) {
        User u = getUserFromNick(channel, nick);
        return u != null && (userIsOwner(nick) || u.hasPriv(priv));
    }

    @Override
    public User getUserFromNick(String channel, String nick) {
        User user = null;
        String prefixlessNick;
        for(User u : getUsers(channel)) {
            prefixlessNick = IRCUtil.getPrefixlessNick(u);
            if(prefixlessNick.equals(nick)) {
                user = u;
                break;
            }
        }
        return user;
    }

    @Override
    public void sendMessages(String target, String[] messages) {
        for(String m : messages) sendMessage(target, m);
    }

    @Override
    public void broadcast(String message) {
        for(String channel : getChannels()) {
            sendMessage(channel, message);
        }
    }

    @Override
    public void incrementBlunderCnt() {
        blunderCount++;
    }

    void initLogging() {
        try {
            PropertyConfigurator.configureAndWatch(loggingPropertiesFile.getAbsolutePath(), 15000L);
            logger.info("Logging initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing logging", e);
        }
    }

    @Override
    public void shutdownNow() {
        logger.warn("SHUTTING DOWN");
        disconnect();
    }
}
