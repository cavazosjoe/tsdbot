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
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.functions.*;
import org.tsd.tsdbot.notifications.*;
import org.tsd.tsdbot.runnable.IRCListenerThread;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.scheduled.LogCleanerJob;
import org.tsd.tsdbot.scheduled.RecapCleanerJob;
import org.tsd.tsdbot.scheduled.SchedulerConstants;
import org.tsd.tsdbot.util.ArchivistUtil;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

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
    private Archivist archivist = null;
    private String name;
    private Properties properties;
    private Scheduler scheduler;

    private PoolingHttpClientConnectionManager poolingManager;
    private CloseableHttpClient httpClient;

    private HashMap<Command, MainFunction> functions = new HashMap<>();

    public boolean debug = false;
    public static long blunderCount = 0;

    public static TSDBot build(String name, String[] channels, boolean debug, Properties properties) {
        if(instance == null)
            instance = new TSDBot(name, channels, debug, properties);
        return instance;
    }

    public static TSDBot getInstance() {
        return instance;
    }

    private TSDBot(String name, String[] channels, boolean debug, Properties properties) {
        
        this.name = name;
        this.debug = debug;
        this.properties = properties;

        setName(name);
        setAutoNickChange(true);
        setLogin("tsdbot");
        
        for(String channel : channels) {
            joinChannel(channel);
            logger.info("Joined channel {}", channel);
        }

        historyBuff = HistoryBuff.build(getChannels());

        try {
            archivist = new Archivist(properties, channels);
            logger.info("Archivist initialized successfully");
        } catch (IOException e) {
            logger.error("Could not initialize Archivist", e);
        }

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

        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();

            JobDetail logCleanerJob = newJob(LogCleanerJob.class)
                    .withIdentity(SchedulerConstants.LOG_JOB_KEY)
                    .usingJobData(SchedulerConstants.LOGS_DIR_FIELD, properties.getProperty("archivist.logs"))
                    .build();

            JobDetail recapCleanerJob = newJob(RecapCleanerJob.class)
                    .withIdentity(SchedulerConstants.RECAP_JOB_KEY)
                    .usingJobData(SchedulerConstants.RECAP_DIR_FIELD, properties.getProperty("archivist.recaps"))
                    .build();

            CronTrigger logCleanerTrigger = newTrigger()
                    .withSchedule(cronSchedule("0 0 4 ? * MON")) //4AM every monday
                    .build();

            CronTrigger recapCleanerTrigger = newTrigger()
                    .withSchedule(cronSchedule("0 0 3 * * ?")) //3AM every day
                    .build();

            scheduler.scheduleJob(logCleanerJob, logCleanerTrigger);
            scheduler.scheduleJob(recapCleanerJob, recapCleanerTrigger);
        } catch (Exception e) {
            logger.error("ERROR INITIALIZING SCHEDULED SERVICES", e);
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
        functions.put(Command.WORKBOT, new Wod());
        functions.put(Command.CATCHUP, new Catchup());

        functions.put(Command.RECAP, archivist);

        OmniPost omniPost = new OmniPost();
        functions.put(Command.DBO_FORUM, omniPost);
        functions.put(Command.DBO_NEWS, omniPost);
        functions.put(Command.HBO_FORUM, omniPost);
        functions.put(Command.HBO_NEWS, omniPost);

        if(twitterManager != null)
            functions.put(Command.TWITTER, new org.tsd.tsdbot.functions.Twitter(twitterManager));

        if(!debug) {
            TSDTV tsdtv = TSDTV.getInstance();
            tsdtv.buildSchedule(scheduler);
            functions.put(Command.TSDTV, tsdtv);
            logger.info("TSDTV initialized successfully");
        }

        mainThread = new java.lang.Thread(this);
        mainThread.start();

    }

    public Scheduler getScheduler() {
        return scheduler;
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

    public Properties getProperties() {
        return properties;
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
            if( (!u.getNick().toLowerCase().contains("bot")) && (!u.getNick().equalsIgnoreCase("tipsfedora")) )
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
                "^s/.+?/[^/]*",
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
                "^\\.sanic$",
                "Sanic function. Retrieves a random page from the Sonic fanfiction wiki",
                "USAGE: .sanic",
                null
        ),

        RECAP(
                "^\\.recap.*",
                "Recap function. Get a personalized review of what you missed",
                "USAGE: .recap [ minutes (integer) ]",
                null
        ),

        WORKBOT(
                "^\\.(wod|workbot|werkbot).*",
                "TSD WorkBot. Get a randomized workout for today, you lazy sack of shit",
                "USAGE: .workbot [ options ]",
                null
        ),

        CATCHUP(
                "^\\.catchup",
                "Catchup function. Get a dramatic summary of recent chat history",
                "USAGE: .catchup",
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
