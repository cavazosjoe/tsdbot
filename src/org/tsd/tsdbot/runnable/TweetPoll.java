package org.tsd.tsdbot.runnable;

import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.TwitterManager;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.util.HashSet;

/**
 * Created by Joe on 2/22/14.
 */
public class TweetPoll implements Runnable {

    private TSDBot bot;
    private TwitterManager twitterManager;
    private String proposedTweet;
    private String proposer;
    private TwitterManager.Tweet replyTo = null;
    private int requiredVotes = -1;
    private int duration = 2; //minutes
    private HashSet<String> ayes = new HashSet<>();


    public TweetPoll(TSDBot bot, String proposer, TwitterManager twitterManager, int numUsers, String proposedTweet, TwitterManager.Tweet replyTo) throws Exception {
        this.bot = bot;
        this.twitterManager = twitterManager;

//        if(replyTo != null && proposedTweet.startsWith("@" + replyTo.getUser().getScreenName()))
//            this.proposedTweet = proposedTweet.replace("@" + replyTo.getUser().getScreenName(),"");
//        else this.proposedTweet = proposedTweet;
        this.proposedTweet = proposedTweet;

        this.proposer = proposer;
        this.requiredVotes = Math.min(numUsers,(int)(3*Math.log(numUsers)));
        this.replyTo = replyTo;

        if(proposedTweet == null || proposedTweet.isEmpty())
            throw new Exception("Proposed tweet cannot be blank");
    }

    @Override
    public void run() {
        handlePollStart();
        try {
            Thread.sleep(duration * 60 * 1000); // 2 minute duration
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        handlePollEnd();
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
        bot.sendLines(lines);
    }

    private void handlePollEnd() {

        bot.setRunningTweetPoll(null);

        int numAyes = ayes.size();

        bot.sendLine("The proposed Tweet received " + numAyes + " out of the necessary " + requiredVotes + " votes.");

        if(numAyes >= requiredVotes) {
            bot.sendLine("Victory! Sending the tweet now...");
            try {
                Status newStatus;
                if(replyTo == null) newStatus = twitterManager.postTweet(proposedTweet);
                else newStatus = twitterManager.postReply(replyTo, proposedTweet);
                bot.sendLine("Tweet successful: " + "https://twitter.com/TSD_IRC/status/" + newStatus.getId());
            } catch (TwitterException e) {
                e.printStackTrace();
                bot.sendLine("There was an error sending the tweet: " + e.getMessage());
            }
        } else {
            bot.sendLine("It's ogre.");
        }
    }

}
