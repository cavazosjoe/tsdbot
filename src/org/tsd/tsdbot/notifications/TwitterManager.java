package org.tsd.tsdbot.notifications;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.util.IRCUtil;
import org.tsd.tsdbot.util.RelativeDate;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Created by Joe on 2/20/14.
 */
public class TwitterManager extends NotificationManager<TwitterManager.Tweet> {

    private static Logger logger = LoggerFactory.getLogger("TwitterManager");

    private static final String SCHOOLY = "Schooly_D";
    private static final String USER_HANDLE = "TSD_IRC";
    private static final long USER_ID = 2349834990l;

    private TSDBot bot;
    private Twitter twitter;
    private TwitterStream stream;
    private HashMap<Long,User> following;

    public TwitterManager(final TSDBot bot, Twitter twitter) throws IOException {
        super(5);
        try {

            this.bot = bot;

            Properties prop = new Properties();
            InputStream fis = TwitterManager.class.getResourceAsStream("/tsdbot.properties");
            prop.load(fis);

            String CONSUMER_KEY = prop.getProperty("twitter.consumer_key");
            String CONSUMER_KEY_SECRET = prop.getProperty("twitter.consumer_key_secret");
            String ACCESS_TOKEN = prop.getProperty("twitter.access_token");
            String ACCESS_TOKEN_SECRET = prop.getProperty("twitter.access_token_secret");

            this.twitter = twitter;
            this.twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_KEY_SECRET);
            this.twitter.setOAuthAccessToken(new AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET));

            logger.info("Twitter API initialized successfully");

            this.following = new HashMap<>();
            Long[] followingIds = ArrayUtils.toObject(twitter.getFriendsIDs(USER_ID, -1).getIDs());
            for(Long id : followingIds) following.put(id, twitter.showUser(id));

            if(!bot.debug) { // disable streaming if in debug mode

                final DelayQueue<DelayedImpl> throttle = new DelayQueue<>();
                throttle.put(new DelayedImpl(60 * 1000)); // delay for a minute

                ConfigurationBuilder cb = new ConfigurationBuilder()
                        .setOAuthConsumerKey(CONSUMER_KEY)
                        .setOAuthConsumerSecret(CONSUMER_KEY_SECRET)
                        .setOAuthAccessToken(ACCESS_TOKEN)
                        .setOAuthAccessTokenSecret(ACCESS_TOKEN_SECRET);

                stream = new TwitterStreamFactory(cb.build()).getInstance();
                stream.addListener(new StatusListener() {
                    @Override
                    public void onStatus(Status status) {

                        // don't display our tweets
                        if(status.getUser().getId() == USER_ID) return;

                        // don't display tweets from people we don't follow
                        if(!following.containsValue(status.getUser())) return;

                        // don't display replies to tweets from people we don't follow
                        if(status.getInReplyToUserId() > 0 && (!following.containsKey(status.getInReplyToUserId()))) return;

                        Tweet newTweet = new Tweet(status);
                        recentNotifications.addFirst(newTweet);
                        trimHistory();

                        for(String channel : bot.getChannels())
                            bot.sendMessage(channel,newTweet.getInline());

                        logger.info("Successfully logged tweet");
                    }

                    @Override
                    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}

                    @Override
                    public void onTrackLimitationNotice(int i) {}

                    @Override
                    public void onScrubGeo(long l, long l2) {}

                    @Override
                    public void onStallWarning(StallWarning stallWarning) {}

                    @Override
                    public void onException(Exception e) {
                        logger.error("Twitter Stream ERROR", e);
                        TSDBot.blunderCount++;
                        try {
                            throttle.take();
                            throttle.put(new DelayedImpl(60 * 1000));
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                });

                FilterQuery fq = new FilterQuery(ArrayUtils.toPrimitive(following.keySet().toArray(new Long[]{})));
                stream.filter(fq);

                logger.info("Twitter Streaming API initialized successfully");

            }

        } catch (TwitterException e) {
            logger.error("Twitter Exception", e);
            TSDBot.blunderCount++;
        }
    }

    public void checkRateLimit() throws TwitterException {
        Map<String, RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus();
        for(String key : rateLimitStatusMap.keySet()) {
            System.out.println(key + " limit: " + rateLimitStatusMap.get(key).getLimit());
            System.out.println(key + " remaining: " + rateLimitStatusMap.get(key).getRemaining());
            System.out.println(key + " seconds til reset: " + rateLimitStatusMap.get(key).getSecondsUntilReset());
        }
    }

    public Status postTweet(String text) throws TwitterException {
        if(text.length() > 140) throw new TwitterException("Must be 140 characters or less");
        return twitter.updateStatus(text);
    }

    public Status postReply(Tweet replyTo, String text) throws TwitterException {

        if(text.startsWith("@")) { // text = @whoever I thought you were dead!
            String[] parts = text.split(" ",2);
            if(parts.length > 1) text = parts[1];
        }

        // text = I thought you were dead!

        text = "@" + replyTo.getStatus().getUser().getScreenName() + " " + text;

        // text = @whoever I thought you were dead!

        if(text.length() > 140) throw new TwitterException("Must be 140 characters or less");

        StatusUpdate reply = new StatusUpdate(text);
        reply.setInReplyToStatusId(replyTo.getStatus().getId());

        return twitter.updateStatus(reply);
    }

    public void follow(String channel, String handle) {
        handle = handle.replace("@","");
        try {
            User followed = twitter.createFriendship(handle);
            if(followed != null) {
                following.put(followed.getId(), followed);
                refreshFollowersFilter();
                bot.sendMessage(channel, "Now following @" + followed.getScreenName());
            }
        } catch (TwitterException e) {
            bot.sendMessage(channel, "I could not follow @" + handle + ". Maybe they don't exist?");
            TSDBot.blunderCount++;
        }
    }

    public void unfollow(String channel, String handle) {
        handle = handle.replace("@","");
        try {
            User unfollowed = twitter.destroyFriendship(handle);
            if(unfollowed != null) {
                following.remove(unfollowed.getId());
                refreshFollowersFilter();
                bot.sendMessage(channel, "No longer following @" + unfollowed.getScreenName());
            }
        } catch (TwitterException e) {
            bot.sendMessage(channel, "I could not unfollow @" + handle + ". Maybe they don't exist?");
            TSDBot.blunderCount++;
        }
    }

    public LinkedList<String> getFollowing() throws TwitterException {
        LinkedList<String> ret = new LinkedList<>();
        for(Long id : following.keySet()) {
            User f = following.get(id);
            ret.add(f.getName() + " (@" + f.getScreenName() + ")");
        }
        return ret;
    }

    private void refreshFollowersFilter() throws TwitterException {
        if(!bot.debug)
            stream.filter(new FilterQuery(ArrayUtils.toPrimitive(following.keySet().toArray(new Long[]{}))));
    }

    @Override
    public LinkedList<Tweet> sweep() {
        return new LinkedList<>();
    }

    public class Tweet extends NotificationEntity {

        private Status status;

        public Tweet(Status status) {
            this.status = status;
        }

        public Status getStatus() {
            return status;
        }

        public String getTrimmedId() {
            String asString = String.valueOf(status.getId());
            return asString.substring(asString.length()-4); // ...4321
        }

        @Override
        public String getInline() {
            return IRCUtil.trimToSingleMsg("[Twitter] " + "[" + status.getUser().getName() + " @" + status.getUser().getScreenName() + "] " + status.getText() + " (" + RelativeDate.getRelativeDate(status.getCreatedAt()) + ") id=" + getTrimmedId()) ;
        }

        @Override
        public String getPreview() {
            setOpened(true);
            return IRCUtil.trimToSingleMsg("[" + status.getUser().getName() + " @" + status.getUser().getScreenName() + "] " + status.getText() + " (" + RelativeDate.getRelativeDate(status.getCreatedAt()) + ") id=" + getTrimmedId()) ;
        }

        @Override
        public String[] getFullText() {
            setOpened(true);
            return IRCUtil.splitLongString(status.getText() + " (" + RelativeDate.getRelativeDate(status.getCreatedAt()) + ")") ;
        }

        @Override
        public String getKey() {
            return "" + status.getId();
        }
    }

    class DelayedImpl implements Delayed {

        private long finishTime;

        public DelayedImpl(long delay) {
            this.finishTime = System.currentTimeMillis() + delay;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(finishTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
