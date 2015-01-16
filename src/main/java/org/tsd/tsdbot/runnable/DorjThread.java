package org.tsd.tsdbot.runnable;

import com.google.inject.Inject;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.ThreadType;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.history.InjectableMsgFilterStrategyFactory;
import org.tsd.tsdbot.history.MessageFilter;
import org.tsd.tsdbot.history.NoCommandsStrategy;
import org.tsd.tsdbot.notifications.TwitterManager;
import twitter4j.Status;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

/**
 * Created by Joe on 1/15/2015.
 */
public class DorjThread extends IRCListenerThread {

    private static final Logger logger = LoggerFactory.getLogger(DorjThread.class);

    private static final int duration = 3; //minutes

    private InjectableMsgFilterStrategyFactory msgFilterFact;
    private HistoryBuff historyBuff;
    private TwitterManager twitterManager;
    private Random random;
    private String starter; //ident of first summoner
    private HashSet<String> summoners = new HashSet<>(); //idents of all summoners

    private LinkedList<String> dorjFormats = new LinkedList<>();

    @Inject
    public DorjThread(TSDBot bot, ThreadManager threadManager, Random random,
                      TwitterManager twitterManager, HistoryBuff historyBuff,
                      InjectableMsgFilterStrategyFactory msgFilterFact) throws Exception {
        super(bot, threadManager);
        if(twitterManager == null) {
            throw new Exception("Twitter not initialized yet, can't start the dorj");
        } else {
            this.twitterManager = twitterManager;
        }
        this.random = random;
        this.listeningRegex = "^\\.dorj$";
        this.historyBuff = historyBuff;
        this.msgFilterFact = msgFilterFact;
        dorjFormats.addAll(Arrays.asList(fmts));
    }

    public void init(String channel, String senderIdent) {
        this.channel = channel;
        this.starter = senderIdent;
    }

    private void handleStart() {
        bot.sendMessage(channel, "Dorj system starting ... [ \u000303ONLINE\u0003 ] ... standing by ...");
    }

    // User object doesn't have ident?
    public void addSummoner(User user, String ident) throws DuplicateSummonerException, OpRequiredException {

        if(summoners.size() == 3 && !user.hasPriv(User.Priv.HALFOP))
            throw new OpRequiredException();

        if (!summoners.add(ident))
            throw new DuplicateSummonerException();

        StringBuilder sb = new StringBuilder();

        if(summoners.size() != 1) {
            // the initial .dorj was handled by handleStart()
            String dorj = null;
            switch (summoners.size()) {
                case 2:
                    dorj = "Left";
                    break;
                case 3:
                    dorj = "Right";
                    break;
                case 4:
                    dorj = "Core";
                    break;
            }

            sb.append(String.format(dorjFormats.remove(random.nextInt(dorjFormats.size())), dorj));

            if(summoners.size() == 3) {
                sb.append(" Call in an op to summon the final dorj!");
            } else if(summoners.size() == 4) {
                sb.append(" I can't believe it! It's \u0002happennninnnngggg!!!!!\u0002");
                synchronized (mutex) {
                    mutex.notify();
                }
            }

            bot.sendMessage(channel, sb.toString());
        }
    }

    private void handleEnd() {
        if(summoners.size() == 4) try {
            // successful dorj
            NoCommandsStrategy strat = new NoCommandsStrategy();
            msgFilterFact.injectStrategy(strat);
            HistoryBuff.Message m = historyBuff.getRandomFilteredMessage(channel, null, MessageFilter.create().addFilter(strat));
            Status tweet = twitterManager.postTweet("@DeeJ_BNG " + m.text);
            bot.sendMessage(channel, "Dorj assembled and deployed: " + "https://twitter.com/TSD_IRC/status/" + tweet.getId());
        } catch (Exception e ) {
            logger.error("Error sending dorj", e);
            bot.sendMessage(channel, "Failed to send the dorj :(");
        } else {
            // unsuccessful
            bot.sendMessage(channel, "Failed to summon the Dorj.");
        }
    }

    @Override
    public ThreadType getThreadType() {
        return ThreadType.DORJ;
    }

    @Override
    public void onMessage(String sender, String login, String hostname, String message) {
        User u = bot.getUserFromNick(channel, sender);
        try{
            addSummoner(u, login);
        } catch (DuplicateSummonerException e) {
            bot.sendMessage(channel, "You can only assume one part of the Dorj, " + sender);
        } catch (OpRequiredException e) {
            bot.sendMessage(channel, "An op is required to complete the Dorj");
        }
    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message) {}

    @Override
    public long getRemainingTime() {
        return 0;
    }

    @Override
    public Object call() throws Exception {
        handleStart();
        synchronized (mutex) {
            try {
                mutex.wait(duration * 60 * 1000);
            } catch (InterruptedException e) {
                logger.info("DorjThread.call() interrupted", e);
            }
        }
        handleEnd();
        manager.removeThread(this);
        return null;
    }

    public class DuplicateSummonerException extends Exception {}
    public class OpRequiredException extends Exception {}

    private static final String [] fmts = new String[]{
            "(lights go dim as the %s Dorj hums to life)",
            "%s Dorj ONLINE...",
            "Bringing %s Dorj online ... [ \u000303OK\u0003 ]",
            "Initiating %s Dorj subsystems...",
            "%s Dorj primed and ready!"
    };
}
