package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.http.client.HttpClient;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import javax.naming.OperationNotSupportedException;
import java.util.LinkedList;

/**
 * Created by Joe on 2/20/14.
 */
public class TwitterManager extends NotificationManager<TwitterManager.Tweet> {

    private static final String CONSUMER_KEY = "f8H6BJg8J6ddnE5IwFROZA";
    private static final String CONSUMER_KEY_SECRET = "CwKXxwsyAlMJYyT1XZCpRZ0OjbwuxTBmQfJwvhcU8";

    private static final String ACCESS_TOKEN = "2349834990-ckfRDk81l1tOdaSBc15A7MVThOCauNFL1D12hSD";
    private static final String ACCESS_TOKEN_SECRET = "EW2vPIdwHZhbGIKPgieyoACqucSfS1lnF2tHfEIiMLwmS";

    public void postTweet() {
        //TODO: not this
        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_KEY_SECRET);
            twitter.setOAuthAccessToken(loadAccessToken());

            String msg = "" + Math.random();
            twitter.updateStatus(msg);
            System.out.print(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static AccessToken loadAccessToken(){
        return new AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
    }

    @Override
    public LinkedList<Tweet> sweep() throws OperationNotSupportedException {
        return null;
    }

    @Override
    public LinkedList<Tweet> sweep(HttpClient client) throws OperationNotSupportedException {
        return null;
    }

    @Override
    public LinkedList<Tweet> sweep(WebClient webClient) throws OperationNotSupportedException {
        return null;
    }

    @Override
    public LinkedList<Tweet> history() {
        return null;
    }

    @Override
    public Tweet expand(String key) {
        return null;
    }

    public class Tweet extends NotificationEntity {

        @Override
        public String getInline() {
            return null;
        }

        @Override
        public String[] getPreview() {
            return new String[0];
        }

        @Override
        public String[] getFullText() {
            return new String[0];
        }
    }
}
