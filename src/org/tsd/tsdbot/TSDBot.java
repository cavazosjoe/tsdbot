package org.tsd.tsdbot;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.functions.*;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.notifications.*;
import org.tsd.tsdbot.runnable.IRCListenerThread;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.util.ArchivistUtil;
import org.tsd.tsdbot.util.FuzzyLogic;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBot extends PircBot {

    private static Logger logger = LoggerFactory.getLogger(TSDBot.class);

    public static long blunderCount = 0;
    public static boolean showNotifications = false;

    private ThreadManager threadManager = new ThreadManager(10);

    private HashMap<Command, MainFunction> functions = new HashMap<>();
    private HashMap<NotificationType, NotificationManager> notificationManagers = new HashMap<>();

    @Inject
    protected HistoryBuff historyBuff;

    @Inject
    protected Archivist archivist;

    public boolean debug = false;

    public TSDBot(String name, String nickservPass, String server, String[] channels, boolean debug) throws IrcException, IOException {
        
        this.debug = debug;

        setName(name);
        setAutoNickChange(true);
        setLogin("tsdbot");
        setVerbose(false);
        setMessageDelay(10); //10 ms
        connect(server);
        if(!StringUtils.isEmpty(nickservPass))
            identify(nickservPass);

        for(String channel : channels) {
            joinChannel(channel);
            logger.info("Joined channel {}", channel);
        }

    }

    @Inject
    public void setFunctionTable(Set<MainFunction> functions) {
        for(MainFunction function : functions) {
            for(Command c : Command.fromFunction(function)) {
                this.functions.put(c, function);
            }
        }
    }

    @Inject
    public void setNotificationTable(Set<NotificationManager> managers) {
        for(NotificationManager manager : managers) {
            for(NotificationType type : NotificationType.fromManager(manager)) {
                this.notificationManagers.put(type, manager);
            }
        }
    }

    public HashMap<NotificationType, NotificationManager> getNotificationManagers() {
        return notificationManagers;
    }

    public ThreadManager getThreadManager() {
        return threadManager;
    }

    public boolean isDebug() {
        return debug;
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

        logger.info("{}: <{}> {}", channel, sender, message);

        archivist.log(channel, ArchivistUtil.getRawMessage(
                System.currentTimeMillis(),
                sender,
                login,
                message
        ));

        List<Command> matchingCommands = Command.fromString(message);
        for(Command c : matchingCommands) {
            functions.get(c).engage(channel, sender, login, message);

            // propagate command to all listening threads
            for(IRCListenerThread listenerThread : threadManager.getThreadsByChannel(channel))
                listenerThread.onMessage(c, sender, login, hostname, message);
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

    public enum Command {
        
        COMMAND_LIST(
                "^\\.cmd$",
                "Have the bot send you a list of commands",
                "USAGE: .cmd",
                null,
                CommandList.class
        ),

        DEEJ(
                "^\\.deej$",
                "DeeJ utility. Picks a random line from the channel history and makes it all fancy and shit",
                "USAGE: .deej",
                null,
                Deej.class
        ),

        GV(
                "^\\.gv.*",
                "The Generally Vague Utility, I guess, but I don't know why you would want to use it, unless you had" +
                        "a good reason, but I guess that goes without saying, even though I never really had to," +
                        "because if I did have to, I would have just done it",
                "USAGE: .gv [pls]",
                null,
                GeeVee.class
        ),

        TSDTV(
                "^\\.tsdtv.*",
                "The TSDTV Streaming Entertainment Value Service",
                "USAGE: .tsdtv [ catalog [<directory>] | play [<movie-name> | <directory> <movie-name>] ]",
                null,
                TSDTV.class
        ),

        FILENAME(
                "^\\.(filename|fname)$",
                "Pull a random entry from the TSD Filenames Database",
                "USAGE: .filename",
                null,
                Filename.class
        ),

        REPLACE(
                "^s/.+?/[^/]*",
                "Replace stuff",
                "USAGE: s/text1/text2",
                null,
                Replace.class
        ),

        CHOOSE(
                "^\\.choose.*",
                "Have the bot choose a random selection for you",
                "USAGE: .choose option1 | option2 [ | option3...]",
                null,
                Chooser.class
        ),

        SHUT_IT_DOWN(
                "^\\.SHUT_IT_DOWN$",
                "SHUT IT DOWN (owner only)",
                "USAGE: SHUT IT DOWN",
                null,
                ShutItDown.class
        ),

        BLUNDER_COUNT(
                "^\\.blunder.*",
                "View, manage, and update the blunder count",
                "USAGE: .blunder [ count | + ]",
                null,
                null
        ),

        TOM_CRUISE(
                "^\\.tc.*",
                "Generate a random Tom Cruise clip or quote",
                "USAGE: .tc [ clip | quote ]",
                null,
                TomCruise.class
        ),

        HBO_FORUM(
                "^\\.hbof.*",
                "HBO Forum utility: browse recent HBO Forum posts",
                "USAGE: .hbof [ list | pv [postId (optional)] ]",
                null,
                OmniPost.class
        ),

        HBO_NEWS(
                "^\\.hbon.*",
                "HBO News utility: browse recent HBO News posts",
                "USAGE: .hbon [ list | pv [postId (optional)] ]",
                null,
                OmniPost.class
        ),

        DBO_FORUM(
                "^\\.dbof.*",
                "DBO Forum utility: browse recent DBO Forum posts",
                "USAGE: .dbof [ list | pv [postId (optional)] ]",
                null,
                OmniPost.class
        ),

        DBO_NEWS(
                "^\\.dbon.*",
                "DBO News utility: browse recent DBO News posts",
                "USAGE: .dbon [ list | pv [postId (optional)] ]",
                null,
                OmniPost.class
        ),

        STRAWPOLL(
                "^\\.poll.*",
                "Strawpoll: propose a question and choices for the chat to vote on",
                "USAGE: .poll <question> ; <duration (integer)> ; choice 1 ; choice 2 [; choice 3 ...]",
                new String[] {"abort"},
                StrawPoll.class
        ),

        VOTE(
                "^\\.vote.*",
                null, // don't show up in the dictionary
                ".vote <number of your choice>",
                null,
                null
        ),

        TWITTER(
                "^\\.tw.*",
                "Twitter utility: send and receive tweets from our exclusive @TSD_IRC Twitter account! Propose tweets" +
                        " for the chat to vote on.",
                "USAGE: .tw [ following | timeline | tweet <message> | reply <reply-to-id> <message> | " +
                        "follow <handle> | unfollow <handle> | propose [ reply <reply-to-id> ] <message> ]",
                new String[] {"abort","aye"},
                org.tsd.tsdbot.functions.Twitter.class
        ),

        FOURCHAN(
                "^\\.(4chan|4ch).*",
                "4chan \"utility\". Currently just retrieves random images from a board you specify",
                "USAGE: .4chan <board>",
                null,
                FourChan.class
        ),

        SANIC(
                "^\\.sanic$",
                "Sanic function. Retrieves a random page from the Sonic fanfiction wiki",
                "USAGE: .sanic",
                null,
                Sanic.class
        ),

        RECAP(
                "^\\.recap",
                "Recap function. Get a dramatic recap of recent chat history",
                "USAGE: .recap [ minutes (integer) ]",
                null,
                Recap.class
        ),

        WORKBOT(
                "^\\.(wod|workbot|werkbot).*",
                "TSD WorkBot. Get a randomized workout for today, you lazy sack of shit",
                "USAGE: .workbot [ options ]",
                null,
                Wod.class
        ),

        CATCHUP(
                "^\\.catchup.*",
                "Catchup function. Get a personalized review of what you missed",
                "USAGE: .catchup",
                null,
                Archivist.class
        ),

        SCAREQUOTE(
                "^\\.quote",
                "Scare quote \"function\"",
                "USAGE: .quote",
                null,
                ScareQuote.class
        );

        private String regex;
        private String desc;
        private String usage;
        private String[] threadCommands; // used by running threads, not entry point
        private Class<? extends MainFunction> functionMap;

        Command(String regex, String desc, String usage, String[] threadCommands, Class<? extends MainFunction> functionMap) {
            this.regex = regex;
            this.desc = desc;
            this.usage = usage;
            this.threadCommands = threadCommands;
            this.functionMap = functionMap;
        }

        public String getDesc() {
            return desc;
        }

        public String getUsage() {
            return usage;
        }

        public String getRegex() { return regex; }

        public Class<? extends MainFunction> getFunctionMap() {
            return functionMap;
        }

        public boolean threadCmd(String cmd) {
            if(threadCommands == null) return false;
            for(String s : threadCommands) {
                if(s.equals(cmd)) return true;
            }
            return false;
        }

        public static List<Command> fromString(String s) {
            LinkedList<Command> matches = new LinkedList<>();
            for(Command c : values()) {
                if(s.matches(c.getRegex()))
                    matches.add(c);
            }
            return matches;
        }

        public static List<Command> fromFunction(MainFunction function) {
            LinkedList<Command> matches = new LinkedList<>();
            for(Command c : values()) {
                if(function.getClass().equals(c.getFunctionMap()))
                    matches.add(c);
            }
            return matches;
        }

    }

    public enum NotificationType {
        HBO_FORUM("HBO Forum",  Command.HBO_FORUM,  HboForumManager.class),
        HBO_NEWS("HBO News",    Command.HBO_NEWS,   HboNewsManager.class),
        DBO_FORUM("DBO Forum",  Command.DBO_FORUM,  DboForumManager.class),
        DBO_NEWS("DBO News",    Command.DBO_NEWS,   DboNewsManager.class),
        TWITTER("Twitter",      Command.TWITTER,    TwitterManager.class);

        private String displayString;
        private Command accessCommand;
        private Class<? extends NotificationManager> managerMap;

        NotificationType(String displayString, Command accessCommand, Class<? extends NotificationManager> managerMap) {
            this.displayString = displayString;
            this.accessCommand = accessCommand;
            this.managerMap = managerMap;
        }

        public String getDisplayString() {
            return displayString;
        }

        public Command getAccessCommand() {
            return accessCommand;
        }

        public Class<? extends NotificationManager> getManagerMap() {
            return managerMap;
        }

        public static NotificationType fromCommand(Command cmd) {
            for(NotificationType type : values()) {
                if(type.accessCommand.equals(cmd)) return type;
            }
            return null;
        }

        public static List<NotificationType> fromManager(NotificationManager manager) {
            LinkedList<NotificationType> matches = new LinkedList<>();
            for(NotificationType type : values()) {
                if(manager.getClass().equals(type.getManagerMap()))
                    matches.add(type);
            }
            return matches;
        }
    }

    public enum ThreadType {
        STRAWPOLL,
        TWEETPOLL
    }
}
