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
import org.tsd.tsdbot.module.Function;

@Singleton
@Function(initialRegex = "^\\.sanic$")
public class Sanic extends MainFunctionImpl {

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
            bot.incrementBlunderCnt();
        } finally {
            post.releaseConnection();
        }
    }

}
