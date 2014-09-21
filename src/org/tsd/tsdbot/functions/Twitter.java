package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.notifications.NotificationEntity;
import org.tsd.tsdbot.notifications.TwitterManager;
import org.tsd.tsdbot.runnable.TweetPoll;
import twitter4j.Status;
import twitter4j.TwitterException;

import java.util.LinkedList;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class Twitter extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Twitter.class);

    private TwitterManager twitterManager;

    @Inject
    public Twitter(TSDBot bot, TwitterManager mgr) {
        super(bot);
        this.twitterManager = mgr;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        TSDBot.Command cmd = TSDBot.Command.TWITTER;

        User user = bot.getUserFromNick(channel, sender);
        boolean isOp = user.hasPriv(User.Priv.OP);

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length == 1) {
            bot.sendMessage(channel, cmd.getUsage());
        } else {
            try {
                String subCmd = cmdParts[1];

                if(cmd.threadCmd(subCmd)) return;

                if(subCmd.equals("following")) {
                    bot.sendMessage(sender,"Here is a list of the people I'm following: ");
                    for(String s : twitterManager.getFollowing()) bot.sendMessage(sender,s);
                } else if(subCmd.equals("timeline")) {
                    if(twitterManager.history().isEmpty()) bot.sendMessage(channel,"I don't have any tweets in my recent history");
                    else for(TwitterManager.Tweet t : twitterManager.history()) bot.sendMessage(channel,t.getInline());
                } else if(cmdParts.length < 3) { // below this clause, 3 args are always required
                    bot.sendMessage(channel, cmd.getUsage());
                } else if(subCmd.equals("tweet") ) {
                    if(!isOp) {
                        bot.sendMessage(channel,"Only ops can use .tw tweet");
                        return;
                    }
                    String tweet = "";
                    for(int i=2 ; i < cmdParts.length ; i++) tweet += (cmdParts[i] + " ");
                    Status postedTweet = twitterManager.postTweet(tweet);
                    bot.sendMessage(channel,"Tweet successful: " + "https://twitter.com/TSD_IRC/status/" + postedTweet.getId());
                    logger.info("[TWITTER] Posted tweet: {}", "https://twitter.com/TSD_IRC/status/" + postedTweet.getId());
                } else if(subCmd.equals("reply")) {
                    if(!isOp) {
                        bot.sendMessage(channel,"Only ops can use .tw reply");
                        return;
                    }
                    String replyToString = cmdParts[2];
                    LinkedList<TwitterManager.Tweet> matchedTweets = twitterManager.getNotificationByTail(replyToString);
                    if(matchedTweets.size() == 0) bot.sendMessage(channel,"Could not find tweet with ID matching" + replyToString + " in recent history");
                    else if(matchedTweets.size() > 1) {
                        String returnString = "Found multiple matching Tweets in recent history:";
                        for(NotificationEntity not : matchedTweets) returnString += (" " + not.getKey());
                        returnString += ". Help me out here";
                        bot.sendMessage(channel,returnString);
                    } else {
                        String tweet = "";
                        for(int i=3 ; i < cmdParts.length ; i++) tweet += (cmdParts[i] + " ");
                        Status postedReply = twitterManager.postReply(matchedTweets.get(0),tweet);
                        bot.sendMessage(channel,"Reply successful: " + "https://twitter.com/TSD_IRC/status/" + postedReply.getId());
                        logger.info("[TWITTER] Posted reply: {}", "https://twitter.com/TSD_IRC/status/" + postedReply.getId());
                    }
                } else if(subCmd.equals("retweet")) {
                    if(!isOp) {
                        bot.sendMessage(channel,"Only ops can use .tw retweet");
                        return;
                    }
                    String retweetString = cmdParts[2];
                    LinkedList<TwitterManager.Tweet> matchedTweets = twitterManager.getNotificationByTail(retweetString);
                    if(matchedTweets.size() == 0) bot.sendMessage(channel,"Could not find tweet with ID matching" + retweetString + " in recent history");
                    else if(matchedTweets.size() > 1) {
                        String returnString = "Found multiple matching Tweets in recent history:";
                        for(NotificationEntity not : matchedTweets) returnString += (" " + not.getKey());
                        returnString += ". Help me out here";
                        bot.sendMessage(channel,returnString);
                    } else {
                        Status retweet = twitterManager.retweet(matchedTweets.get(0));
                        bot.sendMessage(channel,"Retweet successful: " + "https://twitter.com/TSD_IRC/status/" + retweet.getId());
                        logger.info("[TWITTER] Posted retweet: {}", "https://twitter.com/TSD_IRC/status/" + retweet.getId());
                    }
                } else if(subCmd.equals("follow")) {
                    if(!isOp) {
                        bot.sendMessage(channel,"Only ops can use .tw follow");
                        return;
                    }
                    twitterManager.follow(channel, cmdParts[2]);
                    logger.info("[TWITTER] Followed {}", cmdParts[2]);
                } else if(subCmd.equals("unfollow")) {
                    if(!isOp) {
                        bot.sendMessage(channel,"Only ops can use .tw unfollow");
                        return;
                    }
                    twitterManager.unfollow(channel, cmdParts[2]);
                    logger.info("[TWITTER] Unfollowed {}", cmdParts[2]);
                } else if(subCmd.equals("propose")) {

                    TweetPoll currentPoll = (TweetPoll) bot.getThreadManager().getIrcThread(TSDBot.ThreadType.TWEETPOLL, channel);
                    if(currentPoll != null) {
                        bot.sendMessage(channel,"There is already a tweet poll running. It will end in "
                                + (currentPoll.getRemainingTime()/(60*1000)) + " minutes");
                        return;
                    }

                    // .tw propose I propose this tweet
                    // .tw propose reply 1234 I propose this tweet

                    String proposedTweet = "";
                    if(cmdParts[2].equals("reply")) { // proposing a reply to someone

                        if(cmdParts.length == 3) {
                            bot.sendMessage(channel,"Format for proposing a reply: .tw propose reply <reply-to-ID> <message>");
                            return;
                        }

                        String replyToId = cmdParts[3];
                        LinkedList<TwitterManager.Tweet> matchedTweets = twitterManager.getNotificationByTail(replyToId);
                        if(matchedTweets.size() == 0) bot.sendMessage(channel,"Could not find tweet with ID matching " + replyToId + " in recent history");
                        else if(matchedTweets.size() > 1) {
                            String returnString = "Found multiple matching Tweets in recent history:";
                            for(NotificationEntity not : matchedTweets) returnString += (" " + not.getKey());
                            returnString += ". Help me out here";
                            bot.sendMessage(channel,returnString);
                        } else {
                            TwitterManager.Tweet foundTweet = matchedTweets.get(0);
                            for(int i=4 ; i < cmdParts.length ; i++) proposedTweet += (cmdParts[i] + " ");
                            try {
                                currentPoll = new TweetPoll(
                                        bot,
                                        channel,
                                        sender,
                                        twitterManager,
                                        proposedTweet,
                                        foundTweet);
                                bot.getThreadManager().addThread(currentPoll);
                            } catch (Exception e) {
                                bot.sendMessage(channel,e.getMessage());
                                bot.blunderCount++;
                            }
                        }

                    } else { // just a regular tweet

                        for(int i=2 ; i < cmdParts.length ; i++) proposedTweet += (cmdParts[i] + " ");
                        try {
                            currentPoll = new TweetPoll(
                                    bot,
                                    channel,
                                    sender,
                                    twitterManager,
                                    proposedTweet,
                                    null);
                            bot.getThreadManager().addThread(currentPoll);
                        } catch (Exception e) {
                            bot.sendMessage(channel,e.getMessage());
                            bot.blunderCount++;
                        }
                    }

                } else {
                    bot.sendMessage(channel,cmd.getUsage());
                }
            } catch (TwitterException t) {
                bot.sendMessage(channel,"Error: " + t.getMessage());
                bot.blunderCount++;
            }
        }
    }
}
