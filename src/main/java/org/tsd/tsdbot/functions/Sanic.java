package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class Sanic extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Sanic.class);

    private HttpClient httpClient;

    @Inject
    public Sanic(TSDBot bot, HttpClient httpClient) {
        super(bot);
        this.httpClient = httpClient;
        this.description = "Sanic \"fanfunction\". Retrieves a random page from the Sonic fanfiction wiki";
        this.usage = "USAGE: .sanic";
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        final String url = "http://sonicfanon.wikia.com/wiki/Special:Random";
        HttpPost post = new HttpPost(url); // use POST to stop redirect
        try {
            post.setHeader("User-Agent", "Mozilla/4.0");
            CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(post);
            if(response.getStatusLine().getStatusCode() == 302) {
                String redirectUrl = response.getHeaders("Location")[0].getValue();
                bot.sendMessage(channel, redirectUrl);
            }
            EntityUtils.consume(response.getEntity());
        } catch (Exception e) {
            logger.error("sanic() error",e);
            bot.blunderCount++;
        } finally {
            post.releaseConnection();
        }
    }

    @Override
    public String getRegex() {
        return "^\\.sanic$";
    }
}
