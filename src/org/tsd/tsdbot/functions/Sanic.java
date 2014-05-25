package org.tsd.tsdbot.functions;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;

/**
 * Created by Joe on 5/24/14.
 */
public class Sanic implements MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Sanic.class);

    @Override
    public void run(String channel, String sender, String text) {

        TSDBot bot = TSDBot.getInstance();
        CloseableHttpClient httpClient = bot.getHttpClient();

        final String url = "http://sonicfanon.wikia.com/wiki/Special:Random";
        HttpPost post = new HttpPost(url); // use POST to stop redirect
        try {
            post.setHeader("User-Agent", "Mozilla/4.0");
            CloseableHttpResponse response = httpClient.execute(post);
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
}
