package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.http.client.HttpClient;
import twitter4j.*;
import twitter4j.api.FriendsFollowersResources;
import twitter4j.auth.AccessToken;

import javax.naming.OperationNotSupportedException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Joe on 2/20/14.
 */
public class TwitterManager extends NotificationManager {

    /**
     * //TODO: GET THESE CODES OFF GITHUB
     */
    private static final String HANDLE = "TSD_IRC";
    private static final String CONSUMER_KEY = "f8H6BJg8J6ddnE5IwFROZA";
    private static final String CONSUMER_KEY_SECRET = "CwKXxwsyAlMJYyT1XZCpRZ0OjbwuxTBmQfJwvhcU8";

    private static final String ACCESS_TOKEN = "2349834990-ckfRDk81l1tOdaSBc15A7MVThOCauNFL1D12hSD";
    private static final String ACCESS_TOKEN_SECRET = "EW2vPIdwHZhbGIKPgieyoACqucSfS1lnF2tHfEIiMLwmS";

    private Twitter twitter;

    public TwitterManager(Twitter twitter) {
        this.twitter = twitter;
        this.twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_KEY_SECRET);
        this.twitter.setOAuthAccessToken(new AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET));
    }

    // .tw tweet damn this is some good shit
    // .tw follow person
    // .tw unfollow person
    // .tw list

    public void postTweet(String text) throws TwitterException {
        if(text.length() > 140) throw new TwitterException("Must be 140 characters or less");
        twitter.updateStatus(text);
    }

    public void follow(String handle) throws TwitterException {
        twitter.createFriendship(handle);
    }

    public void unfollow(String handle) throws TwitterException {
        twitter.destroyFriendship(handle);
    }

    public List<String> getFollowing() throws TwitterException {
        LinkedList<String> following = new LinkedList<>();
        IDs ids = twitter.getFriendsIDs(HANDLE, -1);
        for(long l : ids.getIDs()) {
            following.add(twitter.showUser(l).getScreenName());
        }
        return following;
    }

    @Override
    public LinkedList<Tweet> sweep() {
        return new LinkedList<>();
    }

    @Override
    public LinkedList<Tweet> history() {
        return new LinkedList<>();
    }

    @Override
    public NotificationOrigin getOrigin() {
        return NotificationOrigin.TWITTER;
    }

    public class Tweet extends NotificationEntity {

        @Override
        public String getInline() {
            return null;
        }

        @Override
        public String getPreview() {
            setOpened(true);
            return null;
        }

        @Override
        public String[] getFullText() {
            setOpened(true);
            return null;
        }

        @Override
        public String getKey() {
            return "";
        }
    }
}
