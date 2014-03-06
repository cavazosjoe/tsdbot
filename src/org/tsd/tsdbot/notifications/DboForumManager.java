package org.tsd.tsdbot.notifications;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.xml.XmlPage;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.util.HtmlSanitizer;
import org.tsd.tsdbot.util.IRCUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 2/18/14.
 */
public class DboForumManager extends NotificationManager<DboForumManager.DboForumPost> {

    private static final String threadsRss = "http://destiny.bungie.org/forum/index.php?mode=rss&items=thread_starts";
    private static final Pattern postIdPattern = Pattern.compile("(\\d+)");
    private static SimpleDateFormat dboSdf; //Thu, 20 Feb 2014 01:59:08 +0000

    private WebClient webClient;

    static {
        HtmlSanitizer.allowedTags = Pattern.compile("^()$");
        HtmlSanitizer.forbiddenTags = Pattern.compile("^(b|p|i|s|a|img|table|thead|tbody|tfoot|tr|th|td|dd|dl|dt|em|h1|h2|h3|h4|h5|h6|li|ul|ol|span|div|strike|strong|"
                + "sub|sup|pre|del|code|blockquote|strike|kbd|br|hr|area|map|object|embed|param|link|form|small|big|script|object|embed|link|style|form|input)$");
    }

    public DboForumManager(WebClient webClient) {
        super(5);
        dboSdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
        dboSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.webClient = webClient;
    }

    @Override
    public LinkedList<DboForumPost> sweep() {
        //new threads found by sweep -- can be larger than MAX_HISTORY but is unlikely
        LinkedList<DboForumPost> notifications = new LinkedList<>();
        try {
            final XmlPage rssPage =  webClient.getPage("http://destiny.bungie.org/forum/index.php?mode=rss&items=thread_starts");
            Document rssDoc = rssPage.getXmlDocument();
            NodeList nlist = rssDoc.getElementsByTagName("item");

            DboForumPost newPost = null;
            for(int i=0 ; i < Math.min(nlist.getLength(),MAX_HISTORY) ; i++) {
                Node n = nlist.item(i);
                if(n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element)n;
                    int postId = getPostNumFromLink(getField(e, "guid"));

                    if((!recentNotifications.isEmpty()) && postId <= recentNotifications.getFirst().getPostId()) break;

                    newPost = new DboForumPost();
                    newPost.setPostId(postId);
                    newPost.setAuthor(getField(e,"dc:creator"));
                    newPost.setSubject(getField(e,"title"));
                    newPost.setDate(dboSdf.parse(getField(e,"pubDate")));
                    newPost.setBody(HtmlSanitizer.sanitize(getField(e,"content:encoded")));

                    notifications.addLast(newPost);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            TSDBot.blunderCount++;
        }

        recentNotifications.addAll(0,notifications);
        trimHistory();
        return notifications;
    }

    private String getField(Element e, String fieldName) {
        return e.getElementsByTagName(fieldName).item(0).getTextContent();
    }

    private int getPostNumFromLink(String url) throws Exception {
        Matcher m = postIdPattern.matcher(url);
        while(m.find()) {
            return Integer.parseInt(m.group(1));
        }
        throw new Exception("Could not parse " + url + " for post ID");
    }

    public class DboForumPost extends NotificationEntity {

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
            return "[DBO Forum] " + "(" + postId + ") " + author + " -- " + subject + " -- " + IRCUtil.shortenUrl("http://destiny.bungie.org/forum/index.php?id=" + postId);
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
