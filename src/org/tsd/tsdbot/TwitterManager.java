package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.client.HttpClient;
import org.tsd.tsdbot.util.IRCUtil;
import org.tsd.tsdbot.util.RelativeDate;
import twitter4j.*;
import twitter4j.api.FriendsFollowersResources;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

import javax.naming.OperationNotSupportedException;
import java.util.*;

/**
 * Created by Joe on 2/20/14.
 */
public class TwitterManager extends NotificationManager<TwitterManager.Tweet> {

    /**
     * //TODO: GET THESE CODES OFF GITHUB
     */
    private static final String USER_HANDLE = "TSD_IRC";
    private static final long USER_ID = 2349834990l;

    private static final String CONSUMER_KEY = "f8H6BJg8J6ddnE5IwFROZA";
    private static final String CONSUMER_KEY_SECRET = "CwKXxwsyAlMJYyT1XZCpRZ0OjbwuxTBmQfJwvhcU8";

    private static final String ACCESS_TOKEN = "2349834990-ckfRDk81l1tOdaSBc15A7MVThOCauNFL1D12hSD";
    private static final String ACCESS_TOKEN_SECRET = "EW2vPIdwHZhbGIKPgieyoACqucSfS1lnF2tHfEIiMLwmS";

    private Twitter twitter;
    private TwitterStream stream;
    private HashSet<Long> following;
    private TSDBot bot;

    // first = most recent
    private static final int MAX_HISTORY = 5;
    private LinkedList<Tweet> recentTweets = new LinkedList<>();

    public TwitterManager(final TSDBot bot, Twitter twitter) {

        try {
            this.twitter = twitter;
            this.bot = bot;
            this.twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_KEY_SECRET);
            this.twitter.setOAuthAccessToken(new AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET));

            ConfigurationBuilder cb = new ConfigurationBuilder()
                    .setOAuthConsumerKey(CONSUMER_KEY)
                    .setOAuthConsumerSecret(CONSUMER_KEY_SECRET)
                    .setOAuthAccessToken(ACCESS_TOKEN)
                    .setOAuthAccessTokenSecret(ACCESS_TOKEN_SECRET);

            stream = new TwitterStreamFactory(cb.build()).getInstance();
            stream.addListener(new StatusListener() {
                @Override
                public void onStatus(Status status) {
                    if(status.getUser().getId() == USER_ID) return;
                    Tweet newTweet = new Tweet(status);
                    recentTweets.addFirst(newTweet);
                    trimHistory();
                    bot.sendLine(newTweet.getInline());
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
                    e.printStackTrace();
                }
            });

            Long[] followingIds = ArrayUtils.toObject(twitter.getFriendsIDs(USER_ID, -1).getIDs());
            following = new HashSet<>(Arrays.asList(followingIds));
            FilterQuery fq = new FilterQuery(ArrayUtils.toPrimitive(following.toArray(new Long[]{})));
            stream.filter(fq);

        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    // .tw tweet damn this is some good shit
    // .tw reply <num> u wot m8
    // .tw follow person
    // .tw unfollow person
    // .tw list

    public void checkRateLimit() throws TwitterException {
        Map<String, RateLimitStatus> rateLimitStatusMap = twitter.getRateLimitStatus();
        for(String key : rateLimitStatusMap.keySet()) {
            System.out.println(key + " limit: " + rateLimitStatusMap.get(key).getLimit());
            System.out.println(key + " remaining: " + rateLimitStatusMap.get(key).getRemaining());
            System.out.println(key + " seconds til reset: " + rateLimitStatusMap.get(key).getSecondsUntilReset());
        }
    }

    public void postTweet(String text) throws TwitterException {
        if(text.length() > 140) throw new TwitterException("Must be 140 characters or less");
        twitter.updateStatus(text);
    }

    public void postReply(Tweet replyTo, String text) throws TwitterException {

        if(text.startsWith("@")) { // text = @whoever I thought you were dead!
            String[] parts = text.split(" ",2);
            if(parts.length > 1) text = parts[1];
        }

        // text = I thought you were dead!

        text = "@" + replyTo.getStatus().getUser().getScreenName() + " " + text;

        // text = @whoever I thought you were dead!

        StatusUpdate reply = new StatusUpdate(text);
        reply.setInReplyToStatusId(replyTo.getStatus().getId());

        twitter.updateStatus(reply);
    }

    public void follow(String handle) throws TwitterException {
        User followed = twitter.createFriendship(handle);
        if(followed != null) {
            following.add(followed.getId());
            refreshFollowersFilter();
        }
    }

    public void unfollow(String handle) throws TwitterException {
        User unfollowed = twitter.destroyFriendship(handle);
        if(unfollowed != null) {
            following.remove(unfollowed.getId());
            refreshFollowersFilter();
        }
    }

    public List<String> getFollowing() throws TwitterException {
        LinkedList<String> following = new LinkedList<>();
        IDs ids = twitter.getFriendsIDs(USER_ID, -1);
        for(long l : ids.getIDs()) {
            following.add(twitter.showUser(l).getScreenName());
        }
        return following;
    }

    private void refreshFollowersFilter() throws TwitterException {
        stream.filter(new FilterQuery(ArrayUtils.toPrimitive(following.toArray(new Long[]{}))));
    }

    @Override
    public LinkedList<Tweet> sweep() {
        return new LinkedList<>();
//        LinkedList<Tweet> notifications = new LinkedList<>();
//        try {
//            Tweet newTweet = null;
//            for (Status status : twitter.getHomeTimeline(new Paging(1,20))) {
//                if(status.getUser().getId() == USER_ID) continue;
//                if(notifications.size() >= 5) break; //flood filter, consider generalizing to other mgrs
//                if((!recentTweets.isEmpty()) && status.getId() <= recentTweets.getFirst().getStatus().getId()) break;
//                newTweet = new Tweet(status);
//                notifications.addLast(newTweet);
//            }
//        } catch (TwitterException e) {
//            e.printStackTrace();
//        }
//        recentTweets.addAll(0,notifications);
//        trimHistory();
//        return notifications;
    }

    private void trimHistory() {
        while(recentTweets.size() > MAX_HISTORY) recentTweets.removeLast();
    }

    @Override
    public LinkedList<Tweet> history() {
        return recentTweets;
    }

    @Override
    public NotificationOrigin getOrigin() {
        return NotificationOrigin.TWITTER;
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
}
