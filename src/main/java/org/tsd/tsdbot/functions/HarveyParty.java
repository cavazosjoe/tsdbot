package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.Function;

import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 5/14/2015.
 */
@Function(initialRegex = "^\\.request.*")
public class HarveyParty extends MainFunctionImpl {

    private static final Logger logger = LoggerFactory.getLogger(HarveyParty.class);

    private HttpClient httpClient;
    private ExecutorService executorService;

    @Inject
    public HarveyParty(Bot bot, HttpClient httpClient, ExecutorService executorService) {
        super(bot);
        this.httpClient = httpClient;
        this.executorService = executorService;
    }

    @Override
    public void run(final String channel, final String sender, String ident, String text) {

        final String[] cmdParts = text.split("\\s+");

        if(cmdParts.length < 2) {
            bot.sendMessage(channel, "USAGE: .request <text>");
            return;
        }

        Future<Integer> future = executorService.submit(new Callable<Integer>(){
            @Override
            public Integer call() throws Exception {

                Integer statusCode = -1;
                HttpGet get = null;
                HttpPost post = null;
                try {

                    get = new HttpGet("http://www.theharveyparty.com/music/");
                    CloseableHttpResponse getResponse = (CloseableHttpResponse) httpClient.execute(get);
                    String pageText = EntityUtils.toString(getResponse.getEntity());

                    String token = null;
                    Pattern tokenPattern = Pattern.compile(".*(token=\\d+).*", Pattern.DOTALL);
                    Matcher tokenMatcher = tokenPattern.matcher(pageText);
                    while(tokenMatcher.find()) {
                        token = tokenMatcher.group(1);
                    }

                    if(StringUtils.isEmpty(token))
                        throw new Exception("Could not parse token from page");

                    EntityUtils.consumeQuietly(getResponse.getEntity());

                    StringBuilder requestText = new StringBuilder();
                    for (int i = 1; i < cmdParts.length; i++) {
                        if (i != 1)
                            requestText.append(" ");
                        requestText.append(cmdParts[i]);
                    }

                    String requester = "TSDIRC " + sender;
                    String content = String.format("requester=%s&song=%s&token=%s",
                            requester,
                            requestText.toString(),
                            token);

                    post = new HttpPost("http://www.theharveyparty.com/music/sendSong.php");
                    post.setEntity(new StringEntity(content));
                    post.addHeader("Content-type", "application/x-www-form-urlencoded");

                    CloseableHttpResponse postResponse = (CloseableHttpResponse) httpClient.execute(post);
                    statusCode = postResponse.getStatusLine().getStatusCode();
                    logger.info("HarveyParty response: {}", statusCode);

                    EntityUtils.consumeQuietly(postResponse.getEntity());

                } catch (Exception e) {
                    logger.error("Error sending song request", e);
                    bot.sendMessage(channel, "Error sending song request");
                } finally {
                    if(get != null)
                        get.releaseConnection();
                    if(post != null)
                        post.releaseConnection();
                }

                return statusCode;
            }
        });

        try {
            Integer status = future.get(10, TimeUnit.SECONDS);
            if(status/100 == 2)
                bot.sendMessage(channel, "Sent ;)");
            else
                bot.sendMessage(channel, "Failed ;)");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            bot.sendMessage(channel, "Something ain't right...");
        }

    }
}
