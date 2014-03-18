package org.tsd.tsdbot.notifications;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.util.HtmlSanitizer;
import org.tsd.tsdbot.util.IRCUtil;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 2/18/14.
 */
public class HboForumManager extends NotificationManager<HboForumManager.HboForumPost> {

    private static Logger logger = LoggerFactory.getLogger(HboForumManager.class);

    private HttpClient client;

    private static final Pattern newThreadPattern = Pattern.compile("<tr><td><a name='m_(\\d+)'");
    private static final Pattern postInfoPattern = Pattern.compile(
            "<div class='msg_headln'>(.*?)</div>.*?<span class='msg_poster'><a.*?>(.*?)</a>.*?" +
                    "<span class=\"msg_date\">(.*?)</span>.*?<div class=\"msg_text\">(.*?)" +
                    "(?:<hr width=\"510\" align=\"left\" size=\"1\">|<div id=\"msg_form\">)", Pattern.DOTALL
    );

    private static SimpleDateFormat hboSdf = null;
    static {
        HtmlSanitizer.allowedTags = Pattern.compile("^()$");
        HtmlSanitizer.forbiddenTags = Pattern.compile("^(b|p|i|s|a|img|table|thead|tbody|tfoot|tr|th|td|dd|dl|dt|em|h1|h2|h3|h4|h5|h6|li|ul|ol|span|div|strike|strong|"
                + "sub|sup|pre|del|code|blockquote|strike|kbd|br|hr|area|map|object|embed|param|link|form|small|big|script|object|embed|link|style|form|input)$");
    }

    public HboForumManager(HttpClient client) {
        super(5);
        hboSdf = new SimpleDateFormat("MM/dd/yy HH:mm a");
        hboSdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        this.client = client;
    }

    @Override
    public LinkedList<HboForumPost> sweep() {

        LinkedList<HboForumPost> notifications = new LinkedList<>();

        HttpContext context = HttpClientContext.create();
        HttpGet indexGet = null;
        HttpResponse indexResponse = null;
        HttpEntity indexEntity = null;

        try {

            Random rand = new Random();
            URI uri = new URIBuilder()
                    .setScheme("http")
                    .setHost("carnage.bungie.org")
                    .setPath("/haloforum/halo.forum.pl")
                    .setParameter("d", "" + rand.nextInt(1000)) // add dummy param to defeat caching
                    .build();

            indexGet = new HttpGet(uri);
            indexGet.setHeader("User-Agent", "Mozilla/4.0");
            indexResponse = client.execute(indexGet, context);
            indexEntity = indexResponse.getEntity();
            String indexText = EntityUtils.toString(indexEntity);
            Matcher indexMatcher = newThreadPattern.matcher(indexText);

            int postId = -1;
            while(indexMatcher.find() && notifications.size() < MAX_HISTORY) {

                HboForumPost foundPost = null;
                HttpGet postGet = null;
                HttpResponse postResponse = null;
                HttpEntity postEntity = null;
                Matcher postMatcher = null;

                try {

                    postId = Integer.parseInt(indexMatcher.group(1));
                    if( (!recentNotifications.isEmpty()) &&
                            (postId <= recentNotifications.getFirst().getPostId() ) ) {
                        continue;
                    }
                    postGet = new HttpGet("http://carnage.bungie.org/haloforum/halo.forum.pl?read=" + postId);
                    postResponse = client.execute(postGet, context);
                    postEntity = postResponse.getEntity();
                    String postText = EntityUtils.toString(postEntity);
                    if(postText.contains("<div class=\"msg_prev\">")) continue; // stale reply
                    postMatcher = postInfoPattern.matcher(postText);
                    while(postMatcher.find()) {

                        foundPost = new HboForumPost();
                        foundPost.setPostId(postId);
                        foundPost.setDate(hboSdf.parse(postMatcher.group(3)));
                        foundPost.setAuthor(postMatcher.group(2));
                        foundPost.setSubject(postMatcher.group(1));

                        String rawBody = postMatcher.group(4);
                        String sanitizedBody = HtmlSanitizer.sanitize(rawBody);
                        sanitizedBody = sanitizedBody.trim();
                        foundPost.setBody(sanitizedBody);

                        notifications.addLast(foundPost);

                    }

                } finally {
                    if(postEntity != null) EntityUtils.consumeQuietly(postEntity);
                }
            }

        } catch (Exception e) {
            logger.error("HboNewsManager sweep() error", e);
            TSDBot.blunderCount++;
        } finally {
            if(indexEntity != null) EntityUtils.consumeQuietly(indexEntity);
        }

        recentNotifications.addAll(0,notifications);
        trimHistory();
        return notifications;
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
            return "[HBO Forum] " + "(" + postId + ") " + author + " -- " + subject + " -- " + IRCUtil.shortenUrl("http://carnage.bungie.org/haloforum/halo.forum.pl?read=" + postId);
        }

        @Override
        public String getPreview() {
            setOpened(true);
            return IRCUtil.trimToSingleMsg(body);
        }

        @Override
        public String[] getFullText() {
            setOpened(true);
            return IRCUtil.splitLongString(body);
        }

        @Override
        public String getKey() {
            return "" + postId;
        }

    }


}
