package org.tsd.tsdbot;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 2/18/14.
 */
public class HboForumManager extends NotificationManager {

    private static final int MAX_HISTORY = 5;
    private static final Pattern newThreadPattern = Pattern.compile("<tr><td><a name='m_(\\d+)'");
    private static final Pattern postInfoPattern = Pattern.compile(
            "<div class='msg_headln'>(.*?)</div>.*?<span class='msg_poster'><a.*?>(.*?)</a>.*?" +
                    "<span class=\"msg_date\">(.*?)</span>.*?<div class=\"msg_text\">(.*?)<hr width=\"510\" " +
                    "align=\"left\" size=\"1\">", Pattern.DOTALL
    );
    private static SimpleDateFormat hboSdf = null;
    private static final PolicyFactory bodyHtmlPolicy = Sanitizers.FORMATTING;

    // first = newest
    private LinkedList<HboForumPost> threadList = new LinkedList<>();

    public HboForumManager() {
        hboSdf = new SimpleDateFormat("MM/dd/yy HH:mm");
        hboSdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    }

    @Override
    public LinkedList<HboForumPost> sweep(HttpClient client) {
        //new threads found by sweep -- can be larger than MAX_HISTORY but is unlikely
        LinkedList<HboForumPost> notifications = new LinkedList<>();
        try {
            HboForumPost foundPost = null;
            HttpGet postGet = null;
            String postResponse = null;
            Matcher postMatcher = null;

            HttpGet indexGet = new HttpGet("http://carnage.bungie.org/haloforum/halo.forum.pl");
            indexGet.setHeader("User-Agent", "Mozilla/4.0");
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String indexResponse = client.execute(indexGet, responseHandler);
            Matcher indexMatcher = newThreadPattern.matcher(indexResponse);

            int postId = -1;
            while(indexMatcher.find()) {
                postId = Integer.parseInt(indexMatcher.group(1));
                if( (!threadList.isEmpty()) &&
                        (postId < threadList.getLast().getPostId() || threadListContainsPost(postId)) ) continue;
                postGet = new HttpGet("http://carnage.bungie.org/haloforum/halo.forum.pl?read=" + postId);
                postResponse = client.execute(postGet, responseHandler);
                if(postResponse.contains("<div class=\"msg_prev\">")) continue; // stale reply
                postMatcher = postInfoPattern.matcher(postResponse);
                while(postMatcher.find()) {
                    foundPost = new HboForumPost();
                    foundPost.setPostId(postId);
                    foundPost.setDate(hboSdf.parse(postMatcher.group(3)));
                    foundPost.setAuthor(postMatcher.group(2));
                    foundPost.setSubject(postMatcher.group(1));

                    String rawBody = postMatcher.group(4);
                    String sanitizedBody = bodyHtmlPolicy.sanitize(rawBody); // TODO: fix this
                    sanitizedBody = sanitizedBody.replaceAll("<BR>"," ").trim();
                    foundPost.setBody(sanitizedBody);

                    notifications.addLast(foundPost);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        threadList.addAll(0,notifications);
        trimHistory();
        return notifications;
    }

    private void trimHistory() {
        while(threadList.size() > MAX_HISTORY) threadList.removeFirst();
    }

    private boolean threadListContainsPost(int postId) {
        for(HboForumPost post : threadList) {
            if(post.getPostId() == postId) return true;
        }
        return false;
    }

    @Override
    public LinkedList<HboForumPost> history() {
        return threadList;
    }

    @Override
    public NotificationEntity expand(String key) {
        return null;
    }

    public class HboForumPost extends NotificationEntity {

        private int postId;
        private Integer parentId;
        private String author;
        private String subject;
        private String body;

        public boolean isOp() {
            return parentId == null;
        }

        public Integer getParentId() {
            return parentId;
        }

        public void setParentId(Integer parentId) {
            this.parentId = parentId;
        }

        public int getPostId() {
            return postId;
        }

        public void setPostId(int postId) {
            this.postId = postId;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        @Override
        public String getInline() {
            return "[HBO Forum] " + "(" + postId + ") " + author + " -- " + subject + " -- http://carnage.bungie.org/haloforum/halo.forum.pl?read=" + postId;
        }

        @Override
        public String getPreview() {
            if(body.length() < 350) return body;
            else return body.substring(0,350) + "... (snip)";
        }

        @Override
        public String getFullText() {
            return body;
        }

    }


}
