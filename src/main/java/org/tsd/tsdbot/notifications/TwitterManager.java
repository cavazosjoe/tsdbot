package org.tsd.tsdbot.notifications;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.NotificationType;
import org.tsd.tsdbot.Stage;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.config.TSDBotConfiguration;
import org.tsd.tsdbot.module.NotifierChannels;
import org.tsd.tsdbot.util.IRCUtil;
import org.tsd.tsdbot.util.RelativeDate;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
public class TwitterManager extends NotificationManager<TwitterManager.Tweet> {

    private static Logger logger = LoggerFactory.getLogger(TwitterManager.class);

    private static final long USER_ID = 2349834990l;
    private static final long EXCEPTION_COOLDOWN = TimeUnit.MINUTES.toMillis(2);
    private static final long COOLDOWN_PERIOD = TimeUnit.HOURS.toMillis(2);

    private Stage stage;
    private Twitter twitter;
    private TwitterStream stream;
    private List<String> channels = new LinkedList<>();

    private final Map<Long, User> following = new HashMap<>();
    private final Map<Long, Long> throttledUsers = new HashMap<>(); // userId -> timestamp of last tweet
    private final DelayQueue<DelayedImpl> exceptionThrottle = new DelayQueue<>();

    @Inject
    public TwitterManager(final TSDBot bot,
                          final Twitter twitter,
                          TSDBotConfiguration configuration,
                          Stage stage,
                          @NotifierChannels Map notifierChannels) throws IOException {
        super(bot, 5, true);
        try {

            this.bot = bot;
            this.stage = stage;
            this.channels = (List<String>) notifierChannels.get("twitter");

            String CONSUMER_KEY =           configuration.twitter.consumerKey;
            String CONSUMER_KEY_SECRET =    configuration.twitter.consumerKeySecret;
            String ACCESS_TOKEN =           configuration.twitter.accessToken;
            String ACCESS_TOKEN_SECRET =    configuration.twitter.accessTokenSecret;

            this.twitter = twitter;
            this.twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_KEY_SECRET);
            this.twitter.setOAuthAccessToken(new AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET));

            logger.info("Twitter API initialized successfully");

            long[] followingIds = twitter.getFriendsIDs(USER_ID, -1).getIDs();
            following.putAll(
                    IntStream.range(0, followingIds.length)
                            .mapToObj(i -> getUserFromId(followingIds[i]))
                            .collect(Collectors.toMap(User::getId, u -> u))
            );

            if(stage.equals(Stage.production)) { // disable streaming if in dev mode

                exceptionThrottle.put(new DelayedImpl(EXCEPTION_COOLDOWN)); // delay for two minutes

                ConfigurationBuilder cb = new ConfigurationBuilder()
                        .setOAuthConsumerKey(CONSUMER_KEY)
                        .setOAuthConsumerSecret(CONSUMER_KEY_SECRET)
                        .setOAuthAccessToken(ACCESS_TOKEN)
                        .setOAuthAccessTokenSecret(ACCESS_TOKEN_SECRET);

                stream = new TwitterStreamFactory(cb.build()).getInstance();
                stream.addListener(new StatusListener() {
                    @Override
                    public void onStatus(Status status) {

                        long userId = status.getUser().getId();
                        logger.debug("Evaluating status: {} (user = {})", status.getId(), userId);

                        // don't display our tweets
                        if(userId == USER_ID) {
                            logger.info("Status is ours, userId = {}", userId);
                            return;
                        }

                        if(status.isRetweet()) {
                            long currentUserRetweetId = status.getCurrentUserRetweetId();
                            if(!following.containsKey(currentUserRetweetId)) {
                                logger.debug("Status is retweet from not-followed user: {}", currentUserRetweetId);
                                return;
                            } else {
                                logger.debug("Status is retweet from followed user: {}", following.get(currentUserRetweetId).getScreenName());
                            }
                        } else {
                            if(!following.containsKey(userId)) {
                                logger.debug("Status is from not-followed user: {}", userId);
                                return;
                            } else {
                                logger.debug("Status is from followed user: {}", following.get(userId).getScreenName());
                            }
                        }

                        // don't display replies to tweets from people we don't follow
                        String text = status.getText();
                        if(StringUtils.isNotBlank(text) && text.startsWith("@")) {
                            logger.debug("Status is probably a reply, discerning user...");
                            boolean foundUser = false;
                            String firstWord = status.getText().split("\\s+")[0]; // @DARKSNIPER99
                            if(firstWord.length() > 1) {
                                final String replyTo = firstWord.substring(1);
                                logger.debug("Checking for user in following list: {}", replyTo);
                                foundUser = following.values()
                                        .parallelStream()
                                        .anyMatch(user -> user.getScreenName().equals(replyTo));
                            }
                            if(!foundUser) {
                                logger.debug("Unable to find user in following list");
                                return; // we're not following whomever this is a reply to
                            } else {
                                logger.debug("We are following this user");
                            }
                        }

                        if(throttledUsers.containsKey(userId)) {
                            // don't display the tweet if the tweeter has tweeted < within cooldown period
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - throttledUsers.get(status.getUser().getId()) < COOLDOWN_PERIOD) {
                                return;
                            } else {
                                throttledUsers.put(userId, currentTime);
                            }
                        }

                        Tweet newTweet = new Tweet(status);
                        recentNotifications.addFirst(newTweet);
                        trimHistory();

                        for(String channel : channels) {
                            bot.sendMessage(channel, newTweet.getInline());
                        }

                        logger.info("Successfully logged tweet: {}", newTweet.getInline());
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
                        bot.incrementBlunderCnt();
                        try {
                            exceptionThrottle.take();
                            exceptionThrottle.put(new DelayedImpl(EXCEPTION_COOLDOWN));
                        } catch (InterruptedException e1) {
                            logger.error("Interrupted during exception throttling", e1);
                        }
                    }
                });

                FilterQuery fq = new FilterQuery(getFollowingIds());
                stream.filter(fq);

                logger.info("Twitter Streaming API initialized successfully");
            }

        } catch (TwitterException e) {
            logger.error("Twitter Exception", e);
            bot.incrementBlunderCnt();
        }
    }

    private User getUserFromId(long id) {
        try {
            return twitter.showUser(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public QueryResult search(String queryString, int limit) throws TwitterException {
        Query q = new Query(queryString);
        if(limit > 0) {
            q.setCount(limit);
        }
        return twitter.search(q);
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
        if(text.length() > 140) {
            throw new TwitterException("Must be 140 characters or less");
        }
        return twitter.updateStatus(text);
    }

    public Status retweet(Tweet toRetweet) throws TwitterException {
        return twitter.retweetStatus(toRetweet.getStatus().getId());
    }

    public Status postReply(Tweet replyTo, String text) throws TwitterException {

        if(text.startsWith("@")) { // text = @whoever I thought you were dead!
            String[] parts = text.split(" ",2);
            if(parts.length > 1) {
                text = parts[1];
            }
        }

        // text = I thought you were dead!

        text = "@" + replyTo.getStatus().getUser().getScreenName() + " " + text;

        // text = @whoever I thought you were dead!

        if(text.length() > 140) {
            throw new TwitterException("Must be 140 characters or less");
        }

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
            bot.incrementBlunderCnt();
        }
    }

    public void unfollow(String channel, String handle) {
        handle = handle.replace("@","");
        try {
            User unfollowed = twitter.destroyFriendship(handle);
            if(unfollowed != null) {
                following.remove(unfollowed.getId());
                throttledUsers.remove(unfollowed.getId());
                refreshFollowersFilter();
                bot.sendMessage(channel, "No longer following @" + unfollowed.getScreenName());
            }
        } catch (TwitterException e) {
            bot.sendMessage(channel, "I could not unfollow @" + handle + ". Maybe they don't exist?");
            bot.incrementBlunderCnt();
        }
    }

    public void unleash(String channel, String handle) {
        handle = handle.replace("@","");
        for(User followed : following.values()) {
            if(followed.getScreenName().equalsIgnoreCase(handle)) {
                throttledUsers.remove(followed.getId());
                bot.sendMessage(channel, "@" + handle + " has been UNLEASHED!");
                return;
            }
        }
        bot.sendMessage(channel, "I could not unleash @" + handle + " because I'm not following xir");
        bot.incrementBlunderCnt();
    }

    public void throttle(String channel, final String handle) {
        User user = following.values().stream()
                .filter(followed -> followed.getScreenName().equalsIgnoreCase(handle))
                .findFirst().orElse(null);

        if(user != null) {
            if(throttledUsers.containsKey(user.getId())) {
                bot.sendMessage(channel, "@" + handle + " is already being throttled");
            } else {
                throttledUsers.put(user.getId(), 0L);
                bot.sendMessage(channel, "@" + handle + " has been restrained");
            }
        } else {
            bot.sendMessage(channel, "I could not throttle @" + handle + " because I'm not following xir");
            bot.incrementBlunderCnt();
        }
    }

    public void delete(String channel, long id) {
        try {
            Status deleted = twitter.destroyStatus(id);
            if(deleted != null) {
                bot.sendMessage(channel, "Successfully deleted status");
            } else {
                bot.sendMessage(channel, "Couldn't delete status. Maybe it doesn't exist?");
            }
        } catch (TwitterException e) {
            String msg = "Error deleting status " + id;
            logger.error(msg, e);
            bot.sendMessage(channel, msg);
        }
    }

    public List<String> getFollowing() throws TwitterException {
        return following.keySet().stream()
                .map(following::get)
                .map(user -> String.format("%s (@%s)", user.getName(), user.getScreenName()))
                .collect(Collectors.toList());
    }

    private void refreshFollowersFilter() throws TwitterException {
        if(stage.equals(Stage.production)) {
            FilterQuery filterQuery = new FilterQuery(getFollowingIds());
            stream.filter(filterQuery);
        }
    }

    private long[] getFollowingIds() {
        return ArrayUtils.toPrimitive(following.keySet().toArray(new Long[following.size()]));
    }

    @Override
    public LinkedList<Tweet> sweep() {
        return new LinkedList<>();
    }

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.TWITTER;
    }

    public class Tweet extends NotificationEntity {

        private static final String inlineFormat = "[Twitter] [%s @%s] %s (%s) id=%s";

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

        private String getRelativeDate() {
            return RelativeDate.getRelativeDate(status.getCreatedAt());
        }

        @Override
        public String getInline() {
            return IRCUtil.trimToSingleMsg(
                    String.format(inlineFormat,
                            status.getUser().getName(),
                            status.getUser().getScreenName(),
                            status.getText(),
                            getRelativeDate(),
                            getTrimmedId()));
        }

        @Override
        public String getPreview() {
            setOpened(true);
            return getInline();
        }

        @Override
        public String[] getFullText() {
            setOpened(true);
            return IRCUtil.splitLongString(String.format("%s (%s)", status.getText(), getRelativeDate()));
        }

        @Override
        public String getKey() {
            return String.valueOf(status.getId());
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
