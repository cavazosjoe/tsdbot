package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.maxsvett.fourchan.FourChan;
import com.maxsvett.fourchan.board.Board;
import com.maxsvett.fourchan.page.Page;
import com.maxsvett.fourchan.post.Post;
import com.maxsvett.fourchan.thread.*;
import com.maxsvett.fourchan.thread.Thread;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.HttpContext;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.database.TSDDatabase;
import org.tsd.tsdbot.notifications.*;
import org.tsd.tsdbot.runnable.IRCListenerThread;
import org.tsd.tsdbot.runnable.StrawPoll;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.runnable.TweetPoll;
import org.tsd.tsdbot.util.HtmlSanitizer;
import org.tsd.tsdbot.util.IRCUtil;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBot extends PircBot implements Runnable {

    private static Logger logger = LoggerFactory.getLogger("TSDBot");

    private java.lang.Thread mainThread;
    private final TSDDatabase database;
    private HashMap<NotificationType, NotificationManager> notificationManagers = new HashMap<>();
    private ThreadManager threadManager = new ThreadManager(10);
    private HistoryBuff historyBuff = new HistoryBuff();
    private String name;

    private CloseableHttpClient httpClient;

    public boolean debug = false;
    public static long blunderCount = 0;

    public TSDBot(String name, String[] channels, boolean debug) {
        
        this.name = name;
        this.debug = debug;

        database = new TSDDatabase();
        database.initialize();
        logger.info("Database initialized successfully");

        setName(name);
        setAutoNickChange(true);
        setLogin("tsdbot");
        
        for(String channel : channels) {
            joinChannel(channel);
            logger.info("Joined channel {}", channel);
        }

        historyBuff.initialize(getChannels());

        PoolingHttpClientConnectionManager poolingManager = new PoolingHttpClientConnectionManager();
        poolingManager.setMaxTotal(10);
        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                if(i >= 5) return false; // don't try more than 5 times
                return e instanceof NoHttpResponseException;
            }
        };
        httpClient = HttpClients.custom().setConnectionManager(poolingManager).setRetryHandler(retryHandler).build();
        logger.info("HttpClient initialized successfully");

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getCookieManager().setCookiesEnabled(true);
        logger.info("WebClient initialized successfully");

        Twitter twitterClient = TwitterFactory.getSingleton();

        try {
            notificationManagers.put(NotificationType.HBO_FORUM, new HboForumManager(httpClient));
            notificationManagers.put(NotificationType.DBO_FORUM, new DboForumManager(webClient));
            notificationManagers.put(NotificationType.HBO_NEWS, new HboNewsManager());
            notificationManagers.put(NotificationType.DBO_NEWS, new DboNewsManager());
            notificationManagers.put(NotificationType.TWITTER, new TwitterManager(this,twitterClient));
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("ERROR INITIALIZING NOTIFICATION MANAGERS", e);
        }

        mainThread = new java.lang.Thread(this);
        mainThread.start();

    }

    @Override
    protected synchronized void onPrivateMessage(String sender, String login, String hostname, String message) {

        if(!message.startsWith(".")) return; //not a command, ignore
        String[] cmdParts = message.split("\\s+");

        Command command = Command.fromString(cmdParts[0]);
        if(command == null) return;

    }

    @Override
    protected synchronized void onMessage(String channel, String sender, String login, String hostname, String message) {

        logger.info("{}: <{}> {}", channel, sender, message);

        if(message.startsWith(".")) {
            String[] cmdParts = message.split("\\s+");

            Command command = Command.fromString(cmdParts[0]);
            if(command == null) return;

            for(IRCListenerThread listenerThread : threadManager.getThreadsByChannel(channel))
                listenerThread.onMessage(command, sender, login, hostname, message);

            switch(command) {
                case COMMAND_LIST: commandList(channel, sender); break;
                case HBO_FORUM: omniPostCmd(command, channel, cmdParts); break;
                case HBO_NEWS: omniPostCmd(command, channel, cmdParts); break;
                case DBO_FORUM: omniPostCmd(command, channel, cmdParts); break;
                case DBO_NEWS: omniPostCmd(command, channel, cmdParts); break;
                case TOM_CRUISE: tc(command, channel, cmdParts); break;
                case STRAWPOLL: poll(command, channel, sender, message.split(";")); break;
                case TWITTER: tw(command, channel, sender, login, cmdParts); break;
                case BLUNDER_COUNT: blunder(command, channel, sender, cmdParts); break;
                case SHUT_IT_DOWN: SHUT_IT_DOWN(channel, sender); break;
                case FOURCHAN: fourChan(command, channel, cmdParts); break;
                case CHOOSE: choose(channel, message); break;
                case FILENAME: filename(channel); break;
            }
        } else {
            String replaceResult = Replacer.tryStringReplace(channel, message, historyBuff);
            if(replaceResult != null)
                sendMessage(channel,replaceResult);
        }

        historyBuff.updateHistory(channel, message, sender);

    }

    private void filename(String channel) {
        HttpGet fnamesGet = null;
        try {

            fnamesGet = new HttpGet("http://teamschoolyd.org/filenames/");
            fnamesGet.setHeader("User-Agent", "Mozilla/4.0");
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response = httpClient.execute(fnamesGet, responseHandler);

            Random rand = new Random();

            Matcher m = Pattern.compile("a href=\"([\\w_]+?\\.\\w{3})\"",Pattern.DOTALL).matcher(response);
            String pfx = "http://www.teamschoolyd.org/filenames/";
            LinkedList<String> all = new LinkedList<>();
            while(m.find()) {
                all.add(m.group(1));
            }
            sendMessage(channel, pfx + all.get(rand.nextInt(all.size())));

        } catch (Exception e) {
            e.printStackTrace();
            blunderCount++;
        } finally {
            if(fnamesGet != null) fnamesGet.releaseConnection();
        }
    }

    private void choose(String channel, String message) {
        sendMessage(channel, Chooser.choose(message));
    }

    private void fourChan(Command command, String channel, String[] cmdParts) {

        if(cmdParts.length != 2) {
            sendMessage(channel, command.getUsage());
            return;
        }

        String boardRegex = "^/??(\\w{1,3})/??$";
        if(!cmdParts[1].matches(boardRegex)) {
            sendMessage(channel, "Could not understand which board you want. Ex: .4chan /v/");
            return;
        }

        Pattern boardPattern = Pattern.compile(boardRegex);
        Matcher boardMatcher = boardPattern.matcher(cmdParts[1]);
        while(boardMatcher.find()) {

            HttpGet indexGet = null;

            try {

                String boardPath = "/" + boardMatcher.group(1) + "/";
                Board board = Board.getBoard(boardPath);
                if(board.isNsfw()) {
                    sendMessage(channel, "I don't support NSFW boards... yet.");
                    return;
                }

                indexGet = new HttpGet("https://a.4cdn.org" + boardPath + "0.json");
                indexGet.setHeader("User-Agent", "Mozilla/4.0");
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String jsonResponse = httpClient.execute(indexGet, responseHandler);

                Page page = FourChan.parsePage(board, jsonResponse);
                Random rand = new Random();
                Thread randomThread = page.getThreads()[1 + rand.nextInt(page.getThreads().length-1)];
                LinkedList<Post> imagePosts = new LinkedList<>();
                imagePosts.add(randomThread.getOP());
                for(Post post : randomThread.getPosts()) {
                    if(post.hasImage()) imagePosts.add(post);
                }

                Post chosen = imagePosts.get(rand.nextInt(imagePosts.size()));
                String comment = IRCUtil.trimToSingleMsg(HtmlSanitizer.sanitize(chosen.getComment().replace("<br>"," ")));
                if(comment != null && (!comment.isEmpty()) && comment.length() < 100)   // also send the comment if
                    sendMessage(channel, comment);                                      // it's small enough
                String ext = chosen.getImageURL().toString().substring(chosen.getImageURL().toString().length() - 4);
                String ret = "(" + chosen.getImageName() + ext + ") " + chosen.getImageURL();

                sendMessage(channel, ret);

            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(channel, "Error retrieving board");
                return;
            } finally {
                if(indexGet != null) indexGet.releaseConnection();
            }
        }
    }

    private void commandList(String channel, String sender) {
        sendMessage(channel, "I'm sending you a message with my list of commands, " + sender);
        boolean first = true;
        for(Command command : Command.values()) {
            if(command.getDesc() != null) {
                if(!first) sendMessage(sender, "-----------------------------------------");
                sendMessage(sender, command.getDesc());
                sendMessage(sender, command.getUsage());
                first = false;
            }
        }
    }

    /**
     * SHUT IT DOWN
     * @SHUT
     * @IT
     * @DOWN
     */
    private void SHUT_IT_DOWN(String channel, String sender) {
        if(!getUserFromNick(channel,sender).hasPriv(User.Priv.SUPEROP)) {
            kick(channel, sender, "Stop that.");
        } else {
            partChannel(channel,"ABORT ABORT ABORT");
        }
    }

    private void blunder(Command command, String channel, String sender, String[] cmdParts) {

        if(cmdParts.length == 1) {
            sendMessage(channel,command.getUsage());
        } else {
            String subCmd = cmdParts[1];
            if(subCmd.equals("count")) { // display the current blunder count
                sendMessage(channel, "Current blunder count: " + blunderCount);
            } else if(subCmd.equals("+") // vv not correct but help em out anyway vv
                    || (cmdParts.length > 2 && cmdParts[1].equals("count") && cmdParts[2].equals("+"))) {
                Random rand = new Random();
                if( (!getUserFromNick(channel,sender).hasPriv(User.Priv.SUPEROP)) && rand.nextDouble() < 0.05 ) {
                    // user is not a super op
                    kick(channel,sender,"R-E-K-T, REKT REKT REKT!");
                    return;
                }
                String[] responses = new String[]{
                        "",                                     "I saw that too. ",
                        "kek. ",                                "My sides are moving on their own. ",
                        "My sides. ",                           "M-muh sides. ",
                        "Wow. ",                                "No argument here. ",
                        "*tip* ",                               "Shit I missed it. Ah well. ",
                        "BLOWN. THE. FUCK. OUT. ",              "B - T - F - O. ",
                        "Shrekt. ",                             "Rekt. ",
                        "[blunders intensify] ",                "What a blunder. ",
                        "Zim-zam status: flim-flammed. "
                };
                String response = String.format(responses[rand.nextInt(responses.length)]
                                    + "Blunder count incremented to %d",++blunderCount);
                sendMessage(channel,response);
            } else {
                sendMessage(channel,command.getUsage());
            }
        }
    }

    private void omniPostCmd(Command command, String channel, String[] cmdParts) {

        NotificationType type = NotificationType.fromCommand(command);
        NotificationManager<NotificationEntity> mgr = notificationManagers.get(type);

        if(cmdParts.length == 1) {
            sendMessage(channel,command.getUsage());
        } else if(cmdParts[1].equals("list")) {
            if(mgr.history() == null || mgr.history().isEmpty())
                sendMessage(channel, "No " + type.getDisplayString() + " posts in recent history");
            for(NotificationEntity notification : mgr.history()) {
                sendMessage(channel,notification.getInline());
            }
        } else if(cmdParts[1].equals("pv")) {
            if(mgr.history().isEmpty()) {
                sendMessage(channel,"No " + type.getDisplayString() +" posts in recent history");
            } else if(cmdParts.length == 2) {
                NotificationEntity mostRecent = mgr.history().getFirst();
                if(mostRecent.isOpened()) sendMessage(channel,"Post " + mostRecent.getKey() + " has already been opened");
                else sendMessage(channel,mostRecent.getPreview());
            } else {
                String postKey = cmdParts[2].trim();
                LinkedList<NotificationEntity> ret = mgr.getNotificationByTail(postKey);
                if(ret.size() == 0) sendMessage(channel,"Could not find " + type.getDisplayString() + " post with ID " + postKey + " in recent history");
                else if(ret.size() > 1) {
                    String returnString = "Found multiple matching " + type.getDisplayString() + " posts in recent history:";
                    for(NotificationEntity not : ret) returnString += (" " + not.getKey());
                    returnString += ". Help me out here";
                    sendMessage(channel,returnString);
                }
                else sendMessage(channel,ret.get(0).getPreview());
            }
        } else {
            sendMessage(channel,command.getUsage());
        }

    }

    private void tc(Command command, String channel, String[] cmdParts) {
        if(cmdParts.length == 1) {
            sendMessage(channel,TomCruise.getRandom(database.getConnection()));
        } else if(cmdParts[1].equals("quote")) {
            sendMessage(channel,TomCruise.getRandomQuote(database.getConnection()));
        } else if(cmdParts[1].equals("clip")) {
            sendMessage(channel,TomCruise.getRandomClip(database.getConnection()));
        } else {
            sendMessage(channel, command.getUsage());
        }
    }

    private void poll(Command command, String channel, String sender, String[] cmdParts) {

        String[] splitOnWhitespace = cmdParts[0].split("\\s+");
        if(splitOnWhitespace.length > 1 && command.threadCmd(splitOnWhitespace[1])) return;

        StrawPoll currentPoll = (StrawPoll) threadManager.getIrcThread(ThreadType.STRAWPOLL, channel);
        if(currentPoll != null) {
            sendMessage(channel,"There is already a poll running. It will end in " + (currentPoll.getRemainingTime()/(60*1000)) + " minute(s)");
            return;
        }

        if(cmdParts.length < 4) {
            sendMessage(channel,Command.STRAWPOLL.getUsage());
            return;
        }

        String question = cmdParts[0].substring(cmdParts[0].indexOf(" ") + 1).trim(); //  .poll This is a question
                                                                                      //        ^--->
        int minutes = Integer.parseInt(cmdParts[1].trim());
        String[] choices = new String[cmdParts.length-2];
        for(int i=0 ; i < choices.length ; i++) {
            choices[i] = cmdParts[i+2].trim();
        }

        try {
            currentPoll = new StrawPoll(
                    this,
                    channel,
                    sender,
                    threadManager,
                    question,
                    minutes,
                    choices
            );
            threadManager.addThread(currentPoll);
        } catch (Exception e) {
            sendMessage(channel,e.getMessage());
            blunderCount++;
        }
    }

    private void tw(Command cmd, String channel, String sender, String login, String[] cmdParts) {

        User user = getUserFromNick(channel,sender);
        boolean isOp = user.hasPriv(User.Priv.OP);

        TwitterManager mgr = (TwitterManager) notificationManagers.get(NotificationType.TWITTER);

        if(cmdParts.length == 1) {
            sendMessage(channel,cmd.getUsage());
        } else {
            try {
                String subCmd = cmdParts[1];

                if(cmd.threadCmd(subCmd)) return;

                if(subCmd.equals("following")) {
                    sendMessage(sender,"Here is a list of the people I'm following: ");
                    for(String s : mgr.getFollowing()) sendMessage(sender,s);
                } else if(subCmd.equals("timeline")) {
                    if(mgr.history().isEmpty()) sendMessage(channel,"I don't have any tweets in my recent history");
                    else for(TwitterManager.Tweet t : mgr.history()) sendMessage(channel,t.getInline());
                } else if(cmdParts.length < 3) { // below this clause, 3 args are always required
                    sendMessage(channel,cmd.getUsage());
                } else if(subCmd.equals("tweet") ) {
                    if(!isOp) {
                        sendMessage(channel,"Only ops can use .tw tweet");
                        return;
                    }
                    String tweet = "";
                    for(int i=2 ; i < cmdParts.length ; i++) tweet += (cmdParts[i] + " ");
                    Status postedTweet = mgr.postTweet(tweet);
                    sendMessage(channel,"Tweet successful: " + "https://twitter.com/TSD_IRC/status/" + postedTweet.getId());
                    logger.info("[TWITTER] Posted tweet: {}", "https://twitter.com/TSD_IRC/status/" + postedTweet.getId());
                } else if(subCmd.equals("reply")) {
                    if(!isOp) {
                        sendMessage(channel,"Only ops can use .tw reply");
                        return;
                    }
                    String replyToString = cmdParts[2];
                    LinkedList<TwitterManager.Tweet> matchedTweets = mgr.getNotificationByTail(replyToString);
                    if(matchedTweets.size() == 0) sendMessage(channel,"Could not find tweet with ID matching" + replyToString + " in recent history");
                    else if(matchedTweets.size() > 1) {
                        String returnString = "Found multiple matching Tweets in recent history:";
                        for(NotificationEntity not : matchedTweets) returnString += (" " + not.getKey());
                        returnString += ". Help me out here";
                        sendMessage(channel,returnString);
                    }
                    else {
                        String tweet = "";
                        for(int i=3 ; i < cmdParts.length ; i++) tweet += (cmdParts[i] + " ");
                        Status postedReply = mgr.postReply(matchedTweets.get(0),tweet);
                        sendMessage(channel,"Reply successful: " + "https://twitter.com/TSD_IRC/status/" + postedReply.getId());
                        logger.info("[TWITTER] Posted reply: {}", "https://twitter.com/TSD_IRC/status/" + postedReply.getId());
                    }
                } else if(subCmd.equals("follow")) {
                    if(!isOp) {
                        sendMessage(channel,"Only ops can use .tw follow");
                        return;
                    }
                    mgr.follow(channel, cmdParts[2]);
                    logger.info("[TWITTER] Followed {}", cmdParts[2]);
                } else if(subCmd.equals("unfollow")) {
                    if(!isOp) {
                        sendMessage(channel,"Only ops can use .tw unfollow");
                        return;
                    }
                    mgr.unfollow(channel, cmdParts[2]);
                    logger.info("[TWITTER] Unfollowed {}", cmdParts[2]);
                } else if(subCmd.equals("propose")) {

                    TweetPoll currentPoll = (TweetPoll) threadManager.getIrcThread(ThreadType.TWEETPOLL, channel);
                    if(currentPoll != null) {
                        sendMessage(channel,"There is already a tweet poll running. It will end in " 
                                + (currentPoll.getRemainingTime()/(60*1000)) + " minutes");
                        return;
                    }

                    // .tw propose I propose this tweet
                    // .tw propose reply 1234 I propose this tweet

                    String proposedTweet = "";
                    if(cmdParts[2].equals("reply")) { // proposing a reply to someone

                        if(cmdParts.length == 3) {
                            sendMessage(channel,"Format for proposing a reply: .tw propose reply <reply-to-ID> <message>");
                            return;
                        }

                        String replyToId = cmdParts[3];
                        LinkedList<TwitterManager.Tweet> matchedTweets = mgr.getNotificationByTail(replyToId);
                        if(matchedTweets.size() == 0) sendMessage(channel,"Could not find tweet with ID matching " + replyToId + " in recent history");
                        else if(matchedTweets.size() > 1) {
                            String returnString = "Found multiple matching Tweets in recent history:";
                            for(NotificationEntity not : matchedTweets) returnString += (" " + not.getKey());
                            returnString += ". Help me out here";
                            sendMessage(channel,returnString);
                        } else {
                            TwitterManager.Tweet foundTweet = matchedTweets.get(0);
                            for(int i=4 ; i < cmdParts.length ; i++) proposedTweet += (cmdParts[i] + " ");
                            try {
                                currentPoll = new TweetPoll(
                                        this,
                                        channel,
                                        threadManager,
                                        sender,
                                        mgr,
                                        getNonBotUsers(channel).size(),
                                        proposedTweet,
                                        foundTweet);
                                threadManager.addThread(currentPoll);
                            } catch (Exception e) {
                                sendMessage(channel,e.getMessage());
                                blunderCount++;
                            }
                        }

                    } else { // just a regular tweet

                        for(int i=2 ; i < cmdParts.length ; i++) proposedTweet += (cmdParts[i] + " ");
                        try {
                            currentPoll = new TweetPoll(
                                    this,
                                    channel,
                                    threadManager,
                                    sender,
                                    mgr,
                                    getNonBotUsers(channel).size(),
                                    proposedTweet,
                                    null);
                            threadManager.addThread(currentPoll);
                        } catch (Exception e) {
                            sendMessage(channel,e.getMessage());
                            blunderCount++;
                        }
                    }

                } else {
                    sendMessage(channel,cmd.getUsage());
                }
            } catch (TwitterException t) {
                sendMessage(channel,"Error: " + t.getMessage());
                blunderCount++;
            }
        }
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

            } catch (Exception e) {
                e.printStackTrace();
                blunderCount++;
            }

            firstPass = false;
        }
    }

    public LinkedList<User> getNonBotUsers(String channel) {
        LinkedList<User> ret = new LinkedList<>();
        for(User u : getUsers(channel)) {
            if(!u.getNick().toLowerCase().contains("tsdbot"))
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
    public void sendMessages(String channel, String[] messages) {
        for(String m : messages) sendMessage(channel, m);
    }

    public enum Command {
        
        COMMAND_LIST(
                new String[]{".cmd"},
                "Have the bot send you a list of commands",
                "USAGE: .cmd",
                null
        ),

        FILENAME(
                new String[]{".filename",".fname"},
                "Pull a random entry from the TSD Filenames Database",
                "USAGE: .filename",
                null
        ),

        CHOOSE(
                new String[]{".choose"},
                "Have the bot choose a random selection for you",
                "USAGE: .choose option1 | option2 [ | option3...]",
                null
        ),

        SHUT_IT_DOWN(
                new String[]{".SHUT_IT_DOWN"},
                "SHUT IT DOWN (owner only)",
                "USAGE: SHUT IT DOWN",
                null
        ),

        BLUNDER_COUNT(
                new String[]{".blunder"},
                "View, manage, and update the blunder count",
                "USAGE: .blunder [ count | + ]",
                null
        ),

        TOM_CRUISE(
                new String[]{".tc"},
                "Generate a random Tom Cruise clip or quote",
                "USAGE: .tc [ clip | quote ]",
                null
        ),

        HBO_FORUM(
                new String[]{".hbof"},
                "HBO Forum utility: browse recent HBO Forum posts",
                "USAGE: .hbof [ list | pv [postId (optional)] ]",
                null
        ),

        HBO_NEWS(
                new String[]{".hbon"},
                "HBO News utility: browse recent HBO News posts",
                "USAGE: .hbon [ list | pv [postId (optional)] ]",
                null
        ),

        DBO_FORUM(
                new String[]{".dbof"},
                "DBO Forum utility: browse recent DBO Forum posts",
                "USAGE: .dbof [ list | pv [postId (optional)] ]",
                null
        ),

        DBO_NEWS(
                new String[]{".dbon"},
                "DBO News utility: browse recent DBO News posts",
                "USAGE: .dbon [ list | pv [postId (optional)] ]",
                null
        ),

        STRAWPOLL(
                new String[]{".poll",".strawpoll"},
                "Strawpoll: propose a question and choices for the chat to vote on",
                "USAGE: .poll <question> ; <duration (integer)> ; choice 1 ; choice 2 [; choice 3 ...]",
                new String[] {"abort"}
        ),

        VOTE(
                new String[]{".vote"},
                null, // don't show up in the dictionary
                ".vote <number of your choice>",
                null
        ),

        TWITTER(
                new String[]{".tw",".twitter"},
                "Twitter utility: send and receive tweets from our exclusive @TSD_IRC Twitter account! Propose tweets" +
                        " for the chat to vote on.",
                "USAGE: .tw [ following | timeline | tweet <message> | reply <reply-to-id> <message> | " +
                        "follow <handle> | unfollow <handle> | propose [ reply <reply-to-id> ] <message> ]",
                new String[] {"abort","aye"}
        ),

        FOURCHAN(
                new String[]{".4chan",".4ch"},
                "4chan \"utility\". Currently just retrieves random images from a board you specify",
                "USAGE: .4chan <board>",
                null
        );

        private String[] aliases;
        private String desc;
        private String usage;
        private String[] threadCommands; // used by running threads, not entry point

        Command(String[] aliases, String desc, String usage, String[] threadCommands) {
            this.aliases = aliases;
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

        public String[] getAliases() {
            return aliases;
        }

        public boolean threadCmd(String cmd) {
            if(threadCommands == null) return false;
            for(String s : threadCommands) {
                if(s.equals(cmd)) return true;
            }
            return false;
        }

        public static Command fromString(String s) {
            for(Command c : values()) {
                for(String alias : c.getAliases())
                    if(alias.equals(s)) return c;
            }
            return null;
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
