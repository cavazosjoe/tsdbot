package org.tsd.tsdbot.runnable;

import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.notifications.TwitterManager;
import twitter4j.Status;
import twitter4j.TwitterException;

import java.util.HashSet;

/**
 * Created by Joe on 2/22/14.
 */
public class TweetPoll extends IRCListenerThread {

    private static Logger logger = LoggerFactory.getLogger("TweetPoll");

    private TwitterManager twitterManager;
    private String proposedTweet;
    private String proposer;
    private TwitterManager.Tweet replyTo = null;
    private int requiredVotes = -1;
    private int duration = 2; //minutes
    private HashSet<String> ayes = new HashSet<>();
    private boolean aborted = false;

    public TweetPoll(TSDBot bot, String channel, ThreadManager threadManager, String proposer, TwitterManager twitterManager, int numUsers, String proposedTweet, TwitterManager.Tweet replyTo) throws Exception {

        super(threadManager, channel);

        this.bot = bot;
        this.twitterManager = twitterManager;
        this.proposedTweet = proposedTweet;
        this.proposer = proposer;

        if(numUsers <= 5) this.requiredVotes = numUsers;
        else this.requiredVotes = (int)(2*Math.log(numUsers));

        this.replyTo = replyTo;

        listeningCommands.add(TSDBot.Command.TWITTER);

        if(proposedTweet == null || proposedTweet.isEmpty())
            throw new Exception("Proposed tweet cannot be blank");
    }

    public String castVote(String voter) {
        if(!ayes.add(voter)) return "You can't vote twice";
        else return null;
    }

    public int getVotesNeeded() {
        return requiredVotes - ayes.size();
    }

    private void handlePollStart() {
        String[] lines = new String[3];
        if(replyTo == null)  lines[0] = proposer + " has proposed we send a new Tweet:";
        else lines[0] = proposer + " has proposed we send a reply to @" + replyTo.getStatus().getUser().getScreenName() + ":";
        lines[1] = proposedTweet;
        lines[2] = "Type \".tw aye\" to vote in favor. " + requiredVotes
                + " votes are required. The voting will end in " + duration + " minutes.";
        bot.sendMessages(channel,lines);
        startTime = System.currentTimeMillis();
        logger.info("BEGINNING TWEET POLL: {}, duration={}, proposedBy={}", proposedTweet, duration, proposer);
    }

    private void handlePollEnd() {

        if(this.aborted) {
            bot.sendMessage(channel,"The tweet proposal has been canceled.");
            logger.info("TWEET POLL CANCELLED: {}", proposedTweet);
            return;
        }

        int numAyes = ayes.size();

        bot.sendMessage(channel,"The proposed Tweet received " + numAyes + " out of the necessary " + requiredVotes + " votes.");

        if(numAyes >= requiredVotes) {
            bot.sendMessage(channel,"Victory! Sending the tweet now...");
            try {
                Status newStatus;
                proposedTweet = "(" + channel + ") "+ proposedTweet; //TODO: use DB to implement channel-specific followings
                if(replyTo == null) newStatus = twitterManager.postTweet(proposedTweet);
                else newStatus = twitterManager.postReply(replyTo, proposedTweet);
                bot.sendMessage(channel,"Tweet successful: " + "https://twitter.com/TSD_IRC/status/" + newStatus.getId());
            } catch (TwitterException e) {
                bot.sendMessage(channel,"There was an error sending the tweet: " + e.getMessage());
                logger.error("ERROR SENDING TWEET POLL TWEET", e);
                TSDBot.blunderCount++;
            }
        } else {
            bot.sendMessage(channel,"It's ogre.");
        }

        logger.info("TWEET POLL FINISHED: {}, duration={}, proposedBy={}", proposedTweet, duration, proposer);
    }

    @Override
    public TSDBot.ThreadType getThreadType() {
        return TSDBot.ThreadType.TWEETPOLL;
    }

    @Override
    public void onMessage(TSDBot.Command command, String sender, String login, String hostname, String message) {
        if(!listeningCommands.contains(command)) return;
        String[] cmdParts = message.split("\\s+");

        String subCmd;

        if(command.equals(TSDBot.Command.TWITTER)) {
            if(cmdParts.length < 2) {
                bot.sendMessage(channel,command.getUsage());
                return;
            }

            synchronized (mutex) {
                subCmd = cmdParts[1];

                if(subCmd.equals("aye")) {
                    String result = castVote(login);
                    String response;
                    if(result == null) {
                        response = "Your vote has been counted, " + sender + ". ";
                        int votesNeeded = getVotesNeeded();
                        if(votesNeeded > 0) {
                            response += (votesNeeded + " more vote(s) needed!");
                            bot.sendMessage(channel, response);
                        }
                        else {
                            response += "No more votes needed! It's happening!";
                            bot.sendMessage(channel,response);
                            mutex.notify();
                        }
                    } else bot.sendMessage(channel, result + ", " + sender);
                } else if(subCmd.equals("abort")) {
                    if(sender.equals(proposer) || bot.getUserFromNick(channel,sender).hasPriv(User.Priv.OP)) {
                        this.aborted = true;
                        mutex.notify();
                    } else {
                        bot.sendMessage(channel,"Only an op or the proposer can abort a tweet. " +
                                "Don't call it a grave, this is the future you chose.");
                    }
                }
            }
        }
    }

    @Override
    public void onPrivateMessage(TSDBot.Command command, String sender, String login, String hostname, String message) {

    }

    @Override
    public long getRemainingTime() {
        return (duration * 60 * 1000) - (System.currentTimeMillis() - startTime);
    }

    @Override
    public Object call() throws Exception {
        handlePollStart();
        synchronized (mutex) {
            try {
                mutex.wait(duration * 60 * 1000);
            } catch (InterruptedException e) {
                logger.info("TweetPoll interrupted!", e);
            }
        }
        handlePollEnd();
        manager.removeThread(this);
        return null;
    }

}
