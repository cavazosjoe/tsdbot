package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.Function;

import java.util.concurrent.*;

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
                try {
                    StringBuilder requestText = new StringBuilder();
                    for (int i = 1; i < cmdParts.length; i++) {
                        if (i != 1)
                            requestText.append(" ");
                        requestText.append(cmdParts[i]);
                    }

                    String requester = "TSDIRC " + sender;
                    String content = String.format("requester=%s&song=%s", requester, requestText.toString());

                    HttpPost post = new HttpPost("http://www.theharveyparty.com/music/sendSong.php");
                    post.setEntity(new StringEntity(content));
                    post.addHeader("Content-type", "application/x-www-form-urlencoded");

                    HttpResponse response = httpClient.execute(post);
                    statusCode = response.getStatusLine().getStatusCode();
                    logger.info("HarveyParty response: {}", statusCode);

                } catch (Exception e) {
                    logger.error("Error sending song request", e);
                    bot.sendMessage(channel, "Error sending song request");
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
