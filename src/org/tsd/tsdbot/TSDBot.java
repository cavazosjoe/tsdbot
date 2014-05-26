package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.functions.*;
import org.tsd.tsdbot.notifications.*;
import org.tsd.tsdbot.runnable.IRCListenerThread;
import org.tsd.tsdbot.runnable.ThreadManager;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBot extends PircBot implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(TSDBot.class);

    private static TSDBot instance = null;

    private java.lang.Thread mainThread;
    private HashMap<NotificationType, NotificationManager> notificationManagers = new HashMap<>();
    private ThreadManager threadManager = new ThreadManager(10);
    private HistoryBuff historyBuff = null;
    private String name;

    private PoolingHttpClientConnectionManager poolingManager;
    private CloseableHttpClient httpClient;

    private HashMap<Command, MainFunction> functions = new HashMap<>();

    public boolean debug = false;
    public static long blunderCount = 0;

    public static TSDBot build(String name, String[] channels, boolean debug) {
        if(instance == null)
            instance = new TSDBot(name, channels, debug);
        return instance;
    }

    public static TSDBot getInstance() {
        return instance;
    }

    private TSDBot(String name, String[] channels, boolean debug) {
        
        this.name = name;
        this.debug = debug;

        setName(name);
        setAutoNickChange(true);
        setLogin("tsdbot");
        
        for(String channel : channels) {
            joinChannel(channel);
            logger.info("Joined channel {}", channel);
        }

        historyBuff = HistoryBuff.build(getChannels());

        poolingManager = new PoolingHttpClientConnectionManager();
        poolingManager.setMaxTotal(100);
        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                if(i >= 5) return false; // don't try more than 5 times
                return e instanceof NoHttpResponseException;
            }
        };
        httpClient = HttpClients.custom()
                .setConnectionManager(poolingManager)
                .setRetryHandler(retryHandler)
                .build();
        logger.info("HttpClient initialized successfully");

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getCookieManager().setCookiesEnabled(true);
        logger.info("WebClient initialized successfully");

        TwitterManager twitterManager = null;
        try {
            Twitter twitterClient = TwitterFactory.getSingleton();
            twitterManager = new TwitterManager(this, twitterClient);
            notificationManagers.put(NotificationType.HBO_FORUM, new HboForumManager(httpClient));
            notificationManagers.put(NotificationType.DBO_FORUM, new DboForumManager(webClient));
            notificationManagers.put(NotificationType.HBO_NEWS, new HboNewsManager());
            notificationManagers.put(NotificationType.DBO_NEWS, new DboNewsManager());
            notificationManagers.put(NotificationType.TWITTER, twitterManager);
        } catch (IOException e) {
            logger.error("ERROR INITIALIZING NOTIFICATION MANAGERS", e);
        }

        /*
         * register functions
         */
        functions.put(Command.CHOOSE, new Chooser());
        functions.put(Command.GV, new GeeVee());
        functions.put(Command.SANIC, new Sanic());
        functions.put(Command.FILENAME, new Filename());
        functions.put(Command.BLUNDER_COUNT, new BlunderCount());
        functions.put(Command.FOURCHAN, new FourChan());
        functions.put(Command.TOM_CRUISE, new TomCruise());
        functions.put(Command.REPLACE, new Replace());
        functions.put(Command.DEEJ, new Deej());
        functions.put(Command.STRAWPOLL, new StrawPoll());

        OmniPost omniPost = new OmniPost();
        functions.put(Command.DBO_FORUM, omniPost);
        functions.put(Command.DBO_NEWS, omniPost);
        functions.put(Command.HBO_FORUM, omniPost);
        functions.put(Command.HBO_NEWS, omniPost);

        if(twitterManager != null)
            functions.put(Command.TWITTER, new org.tsd.tsdbot.functions.Twitter(twitterManager));

        if(!debug) {
            TSDTV tsdtv = TSDTV.getInstance();
            tsdtv.buildSchedule();
            functions.put(Command.TSDTV, tsdtv);
            logger.info("TSDTV initialized successfully");
        }

        mainThread = new java.lang.Thread(this);
        mainThread.start();

    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public ThreadManager getThreadManager() {
        return threadManager;
    }

    public HashMap<NotificationType, NotificationManager> getNotificationManagers() {
        return notificationManagers;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    protected synchronized void onPrivateMessage(String sender, String login, String hostname, String message) {

    }

    @Override
    protected synchronized void onMessage(String channel, String sender, String login, String hostname, String message) {

        logger.info("{}: <{}> {}", channel, sender, message);

        List<Command> matchingCommands = Command.fromString(message);
        for(Command c : matchingCommands) {
            functions.get(c).run(channel, sender, message);

            // propagate command to all listening threads
            for(IRCListenerThread listenerThread : threadManager.getThreadsByChannel(channel))
                listenerThread.onMessage(c, sender, login, hostname, message);
        }

        historyBuff.updateHistory(channel, message, sender);

    }

    @Override
    public synchronized void run() {

        boolean firstPass = true; //TODO: use DB to avoid this

        while(true) {
            try {
                wait(5 * 60 * 1000); // check every 5 minutes
            } catch (InterruptedException e) {
                // something notified this thread, panic.blimp
            }

            try {
                for(NotificationManager<NotificationEntity> sweeper : notificationManagers.values()) {
                    for(NotificationEntity notification : sweeper.sweep()) {
                        if(!firstPass) for(String chan : getChannels()) {
                            sendMessage(chan,notification.getInline());
                        }
                    }
                }

                poolingManager.closeIdleConnections(60, TimeUnit.SECONDS);

            } catch (Exception e) {
                logger.error("TSDBot.run() error", e);
                blunderCount++;
            }

            firstPass = false;
        }
    }

    public LinkedList<User> getNonBotUsers(String channel) {
        LinkedList<User> ret = new LinkedList<>();
        for(User u : getUsers(channel)) {
            if( (!u.getNick().toLowerCase().contains("tsdbot")) && (!u.getNick().equalsIgnoreCase("tipsfedora")) )
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
                null
        ),

        DEEJ(
                "^\\.deej$",
                "DeeJ utility. Picks a random line from the channel history and makes it all fancy and shit",
                "USAGE: .deej",
                null
        ),

        GV(
                "^\\.gv.*",
                "The Generally Vague Utility, I guess, but I don't know why you would want to use it, unless you had" +
                        "a good reason, but I guess that goes without saying, even though I never really had to," +
                        "because if I did have to, I would have just done it",
                "USAGE: .gv [pls]",
                null
        ),

        TSDTV(
                "^\\.tsdtv.*",
                "The TSDTV Streaming Entertainment Value Service",
                "USAGE: .tsdtv [ catalog [<directory>] | play [<movie-name> | <directory> <movie-name>] ]",
                null
        ),

        FILENAME(
                "^\\.(filename|fname)$",
                "Pull a random entry from the TSD Filenames Database",
                "USAGE: .filename",
                null
        ),

        REPLACE(
                "^s/.*?/[^/]*",
                "Replace stuff",
                "USAGE: s/text1/text2",
                null
        ),

        CHOOSE(
                "^\\.choose.*",
                "Have the bot choose a random selection for you",
                "USAGE: .choose option1 | option2 [ | option3...]",
                null
        ),

        SHUT_IT_DOWN(
                "^\\.SHUT_IT_DOWN$",
                "SHUT IT DOWN (owner only)",
                "USAGE: SHUT IT DOWN",
                null
        ),

        BLUNDER_COUNT(
                "^\\.blunder.*",
                "View, manage, and update the blunder count",
                "USAGE: .blunder [ count | + ]",
                null
        ),

        TOM_CRUISE(
                "^\\.tc.*",
                "Generate a random Tom Cruise clip or quote",
                "USAGE: .tc [ clip | quote ]",
                null
        ),

        HBO_FORUM(
                "^\\.hbof.*",
                "HBO Forum utility: browse recent HBO Forum posts",
                "USAGE: .hbof [ list | pv [postId (optional)] ]",
                null
        ),

        HBO_NEWS(
                "^\\.hbon.*",
                "HBO News utility: browse recent HBO News posts",
                "USAGE: .hbon [ list | pv [postId (optional)] ]",
                null
        ),

        DBO_FORUM(
                "^\\.dbof.*",
                "DBO Forum utility: browse recent DBO Forum posts",
                "USAGE: .dbof [ list | pv [postId (optional)] ]",
                null
        ),

        DBO_NEWS(
                "^\\.dbon.*",
                "DBO News utility: browse recent DBO News posts",
                "USAGE: .dbon [ list | pv [postId (optional)] ]",
                null
        ),

        STRAWPOLL(
                "^\\.poll.*",
                "Strawpoll: propose a question and choices for the chat to vote on",
                "USAGE: .poll <question> ; <duration (integer)> ; choice 1 ; choice 2 [; choice 3 ...]",
                new String[] {"abort"}
        ),

        VOTE(
                "^\\.vote.*",
                null, // don't show up in the dictionary
                ".vote <number of your choice>",
                null
        ),

        TWITTER(
                "^\\.tw.*",
                "Twitter utility: send and receive tweets from our exclusive @TSD_IRC Twitter account! Propose tweets" +
                        " for the chat to vote on.",
                "USAGE: .tw [ following | timeline | tweet <message> | reply <reply-to-id> <message> | " +
                        "follow <handle> | unfollow <handle> | propose [ reply <reply-to-id> ] <message> ]",
                new String[] {"abort","aye"}
        ),

        FOURCHAN(
                "^\\.(4chan|4ch).*",
                "4chan \"utility\". Currently just retrieves random images from a board you specify",
                "USAGE: .4chan <board>",
                null
        ),

        SANIC(
                "^\\.sanic.*",
                "Sanic function. Retrieves a random page from the Sonic fanfiction wiki",
                "USAGE: .sanic",
                null
        );

        private String regex;
        private String desc;
        private String usage;
        private String[] threadCommands; // used by running threads, not entry point

        Command(String regex, String desc, String usage, String[] threadCommands) {
            this.regex = regex;
            this.desc = desc;
            this.usage = usage;
            this.threadCommands = threadCommands;
        }

        public String getDesc() {
            return desc;
        }

        public String getUsage() {
            return usage;
        }

        public String getRegex() { return regex; }

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

    }

    public enum NotificationType {
        HBO_FORUM("HBO Forum", Command.HBO_FORUM),
        HBO_NEWS("HBO News", Command.HBO_NEWS),
        DBO_FORUM("DBO Forum", Command.DBO_FORUM),
        DBO_NEWS("DBO News", Command.DBO_NEWS),
        TWITTER("Twitter", Command.TWITTER);

        private String displayString;
        private Command accessCommand;

        NotificationType(String displayString, Command accessCommand) {
            this.displayString = displayString;
            this.accessCommand = accessCommand;
        }

        public String getDisplayString() {
            return displayString;
        }

        public Command getAccessCommand() {
            return accessCommand;
        }

        public static NotificationType fromCommand(Command cmd) {
            for(NotificationType type : values()) {
                if(type.accessCommand.equals(cmd)) return type;
            }
            return null;
        }
    }

    public enum ThreadType {
        STRAWPOLL,
        TWEETPOLL
    }
}
