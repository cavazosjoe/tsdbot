package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jibble.pircbot.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.ThreadType;
import org.tsd.tsdbot.notifications.NotificationEntity;
import org.tsd.tsdbot.notifications.TwitterManager;
import org.tsd.tsdbot.runnable.InjectableIRCThreadFactory;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.runnable.TweetPoll;
import org.tsd.tsdbot.util.IRCUtil;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.TwitterException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class Twitter extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Twitter.class);

    private ThreadManager threadManager;
    private InjectableIRCThreadFactory threadFactory;
    private TwitterManager twitterManager;
    private String mashapeKey;
    private Random random;

    @Inject
    public Twitter(TSDBot bot, TwitterManager mgr, ThreadManager threadManager, Random random,
                   InjectableIRCThreadFactory threadFactory, @Named("mashapeKey") String mashapeKey) {
        super(bot);
        this.description = "Twitter utility: send and receive tweets from our exclusive @TSD_IRC Twitter account! " +
                "Propose tweets for the chat to vote on.";
        this.usage = "USAGE: .tw [ following | timeline | tweet <message> | reply <reply-to-id> <message> | " +
                "follow <handle> | unfollow <handle> | propose [ reply <reply-to-id> ] <message> ]";
        this.twitterManager = mgr;
        this.threadManager = threadManager;
        this.threadFactory = threadFactory;
        this.mashapeKey = mashapeKey;
        this.random = random;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        // this is an operation on a running poll, disregard
        if(TweetPoll.TweetPollOperation.fromString(text) != null)
            return;

        User user = bot.getUserFromNick(channel, sender);
        boolean isOp = user.hasPriv(User.Priv.OP);

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length == 1) {
            bot.sendMessage(channel, usage);
        } else {
            try {
                String subCmd = cmdParts[1];

                if(subCmd.equals("following")) {

                    bot.sendMessage(sender,"Here is a list of the people I'm following: ");
                    for(String s : twitterManager.getFollowing())
                        bot.sendMessage(sender,s);

                } else if(subCmd.equals("timeline")) {

                    if(twitterManager.history().isEmpty())
                        bot.sendMessage(channel,"I don't have any tweets in my recent history");
                    else
                        for(TwitterManager.Tweet t : twitterManager.history())
                            bot.sendMessage(channel,t.getInline());

                } else if(cmdParts.length < 3) { // below this clause, 3 args are always required

                    bot.sendMessage(channel, usage);

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

                } else if(subCmd.equals("unleash")) {

                    if(!isOp) {
                        bot.sendMessage(channel,"Only ops can use .tw unleash");
                        return;
                    }
                    twitterManager.unleash(channel, cmdParts[2]);
                    logger.info("[TWITTER] Unleashed {}", cmdParts[2]);

                } else if(subCmd.equals("throttle")) {

                    if(!isOp) {
                        bot.sendMessage(channel,"Only ops can use .tw throttle");
                        return;
                    }
                    twitterManager.throttle(channel, cmdParts[2]);
                    logger.info("[TWITTER] Throttled {}", cmdParts[2]);

                } else if(subCmd.equals("delete")){

                    if(!isOp) {
                        bot.sendMessage(channel, "Only ops can use .tw delete");
                        return;
                    }

                    long statusToDelete = Long.parseLong(cmdParts[2]);
                    twitterManager.delete(channel, statusToDelete);
                    logger.info("[TWITTER] Deleted {}", statusToDelete);

                } else if(subCmd.equals("propose")) {

                    TweetPoll currentPoll = (TweetPoll) threadManager.getIrcThread(ThreadType.TWEETPOLL, channel);
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
                                currentPoll = threadFactory.newTweetPoll(channel, sender, proposedTweet, foundTweet);
                                threadManager.addThread(currentPoll);
                            } catch (Exception e) {
                                bot.sendMessage(channel,e.getMessage());
                                bot.blunderCount++;
                            }
                        }

                    } else { // just a regular tweet

                        for(int i=2 ; i < cmdParts.length ; i++) proposedTweet += (cmdParts[i] + " ");
                        try {
                            currentPoll = threadFactory.newTweetPoll(channel, sender, proposedTweet, null);
                            threadManager.addThread(currentPoll);
                        } catch (Exception e) {
                            bot.sendMessage(channel,e.getMessage());
                            bot.blunderCount++;
                        }
                    }

                } else if(subCmd.equals("search")) {

                    if(cmdParts.length < 3) {
                        bot.sendMessage(channel, "USAGE: .tw search [query]");
                        return;
                    }

                    StringBuilder queryBuilder = new StringBuilder();
                    for(int i=2 ; i < cmdParts.length ; i++) {
                        if(i != 2)
                            queryBuilder.append(" ");
                        queryBuilder.append(cmdParts[i]);
                    }

                    QueryResult result = twitterManager.search(queryBuilder.toString(), 50);
                    int count = result.getCount();
                    if(count < 1 || result.getTweets().size() < 1) {
                        bot.sendMessage(channel, "Couldn't find any tweets for that query");
                        return;
                    }

                    HttpResponse<JsonNode> response;
                    JSONObject detection ;
                    Status evaluatingTweet;
                    Status chosenTweet = null;
                    String language;
                    boolean reliable;
//                    double confidence = 0;
                    int i=0;
                    LinkedList<Status> tweets = new LinkedList<>(result.getTweets());
                    Collections.shuffle(tweets);
                    try {
                        while (chosenTweet == null && i < count) {
                            evaluatingTweet = tweets.get(i);
                            // discard tweets that are replies or retweets
                            if(evaluatingTweet.getInReplyToUserId() < 0
                                    && evaluatingTweet.getInReplyToStatusId() < 0
                                    && !evaluatingTweet.isRetweet()) {
                                response = Unirest.post("https://community-language-detection.p.mashape.com/detect?key=8de41710a3d110c42095b6b87ee7ad5e")
                                        .header("X-Mashape-Key", mashapeKey)
                                        .header("Content-Type", "application/x-www-form-urlencoded")
                                        .header("Accept", "application/json")
                                        .field("q", evaluatingTweet.getText())
                                        .asJson();
                                detection = response.getBody()
                                        .getObject().getJSONObject("data").getJSONArray("detections").getJSONObject(0);
                                language = detection.getString("language");
                                reliable = detection.getBoolean("isReliable");
//                            confidence = detection.getDouble("confidence");
                                if (language.equals("en")&& reliable)
                                    chosenTweet = evaluatingTweet;
                            }
                            i++;
                        }

                        if(chosenTweet == null) {
                            bot.sendMessage(channel, "Searched " + i + " tweets but couldn't find any English ones");
                        } else {

                            StringBuilder urlBuilder = new StringBuilder();
                            urlBuilder.append("http://twitter.com/").append(chosenTweet.getUser().getScreenName())
                                    .append("/status/").append(chosenTweet.getId());
                            String shortUrl = IRCUtil.shortenUrl(urlBuilder.toString());

                            StringBuilder twBuilder = new StringBuilder();
                            twBuilder.append("@").append(chosenTweet.getUser().getScreenName())
                                    .append(": ").append(chosenTweet.getText()).append(" -- ").append(shortUrl);

                            bot.sendMessage(channel, twBuilder.toString());
                        }

                    } catch (UnirestException e) {
                        logger.error("Error detecting language of tweet", e);
                        bot.sendMessage(channel, "Error detecting tweet language");
                    } catch (Exception e) {
                        logger.error("Error filtering tweets", e);
                        bot.sendMessage(channel, "Error filtering tweets");
                    }

                } else {
                    bot.sendMessage(channel, usage);
                }
            } catch (TwitterException t) {
                bot.sendMessage(channel,"Error: " + t.getMessage());
                bot.blunderCount++;
            }
        }
    }

    @Override
    public String getRegex() {
        return "^\\.tw.*";
    }
}
