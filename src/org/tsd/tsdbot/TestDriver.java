package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.host.xml.XMLDocument;
import com.gargoylesoftware.htmlunit.xml.XmlPage;
import it.sauronsoftware.feed4j.FeedParser;
import it.sauronsoftware.feed4j.bean.Feed;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.tsd.tsdbot.util.HtmlSanitizer;
import org.tsd.tsdbot.util.IRCUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Created by Joe on 2/19/14.
 */
public class TestDriver {

    public static void main(String[] args) throws Exception {
//        HtmlSanitizer.allowedTags = Pattern.compile("^()$");
//        HtmlSanitizer.forbiddenTags = Pattern.compile("^(b|p|i|s|a|img|table|thead|tbody|tfoot|tr|th|td|dd|dl|dt|em|h1|h2|h3|h4|h5|h6|li|ul|ol|span|div|strike|strong|"
//                + "sub|sup|pre|del|code|blockquote|strike|kbd|br|hr|area|map|object|embed|param|link|form|small|big|script|object|embed|link|style|form|input)$");
//        String toSanitize = "<BR>1. Make Halo 5 compatible with 3D TVs.\n" +
//                "<BR>2. <a href=\"http://i.imgur.com/UCfyAy9.jpg\">Just do this but with Cortana</a>\n" +
//                "<BR>3. Massive Profit even Schooly D can't ignore\n" +
//                "<BR>\n" +
//                " </div>\n" +
//                "</div>\n" +
//                "<hr width=\"510\" align=\"left\" size=\"1\">";
//        String result = HtmlSanitizer.sanitize(toSanitize);

        //---------------

//        String longQuote = "Someday? Someday my dream will come? One night you will wake up and discover it never happened. It's " +
//                "all turned around on you. It never will. Suddenly you are old. Didn't happen, and it never will, " +
//                "because you were never going to do it anyway. You'll push it into memory and then zone out in " +
//                "your barco lounger, being hypnotized by daytime TV for the rest of your life. Don't you talk to " +
//                "me about murder. All it ever took was a down payment on a Lincoln town car. That girl, you " +
//                "can't even call that girl. What the fuck are you still doing driving a cab? (Collateral)";
//
//        String[] lines = IRCUtil.splitLongString(longQuote);

        //---------------

//        CloseableHttpClient httpClient = HttpClients.createMinimal();
//        HttpGet indexGet = new HttpGet("http://destiny.bungie.org/forum/index.php?mode=rss&items=thread_starts");
//        indexGet.setHeader("User-Agent", "Mozilla/4.0");
//        ResponseHandler<String> responseHandler = new BasicResponseHandler();
//        String indexResponse = httpClient.execute(indexGet, responseHandler);
//        Feed feed = FeedParser.parse(url);

//        final WebClient webClient = new WebClient(BrowserVersion.CHROME);
//        webClient.getCookieManager().setCookiesEnabled(true);
//        final XmlPage rssPage =  webClient.getPage("http://destiny.bungie.org/forum/index.php?mode=rss&items=thread_starts");
//        Document rssDoc = rssPage.getXmlDocument();
//        NodeList nlist = rssDoc.getElementsByTagName("item");
//        for(int i=0 ; i < nlist.getLength() ; i++) {
//            Node n = nlist.item(i);
//            if(n.getNodeType() == Node.ELEMENT_NODE) {
//                Element e = (Element)n;
//                System.out.println(e.getElementsByTagName("title").item(0).getTextContent());
//            }
//        }

        //---------------------

        TwitterManager twitterManager = new TwitterManager();
        twitterManager.postTweet();

    }
}
