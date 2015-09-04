package org.tsd.tsdbot.functions;

import com.gargoylesoftware.htmlunit.WebClient;
import com.google.inject.Inject;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.module.Function;

import java.util.concurrent.ExecutorService;

/**
 * Created by Joe on 5/14/2015.
 */
@Function(initialRegex = "^\\.request.*")
public class HarveyParty extends MainFunctionImpl {

    private static final Logger logger = LoggerFactory.getLogger(HarveyParty.class);

    private WebClient webClient;
    private HttpClient httpClient;
    private ExecutorService executorService;

    @Inject
    public HarveyParty(Bot bot, HttpClient httpClient, ExecutorService executorService, WebClient webClient) {
        super(bot);
        this.httpClient = httpClient;
        this.executorService = executorService;
        this.webClient = webClient;
    }

    @Override
    public void run(final String channel, final String sender, String ident, String text) {

        final String[] cmdParts = text.split("\\s+");

        if(cmdParts.length < 2) {
            bot.sendMessage(channel, "USAGE: .request <text>");
            return;
        }

        bot.sendMessage(channel, "https://www.youtube.com/watch?v=L1eBuuJqc44");

        /* begin anti-schroogling countermeasures */

//        try {
//
//            webClient.getCookieManager().setCookiesEnabled(true);
//
//            HtmlPage indexPage = webClient.getPage("http://www.theharveyparty.com/music/");
//            webClient.waitForBackgroundJavaScript(1000 * 10);
//            String indexText = indexPage.asText();
//            logger.info("Before submission:\n{}", indexText);
//
//            StringBuilder requestText = new StringBuilder();
//            for (int i = 1; i < cmdParts.length; i++) {
//                if (i != 1)
//                    requestText.append(" ");
//                requestText.append(cmdParts[i]);
//            }
//
//            final HtmlTextInput requesterField = (HtmlTextInput) indexPage.getElementById("requester");
//            requesterField.setValueAttribute(sender);
//
//            final HtmlTextInput songField = (HtmlTextInput) indexPage.getElementById("song");
//            songField.setValueAttribute(requestText.toString());
//
//            final HtmlButton button = (HtmlButton) ((ArrayList) indexPage.getByXPath("//button")).get(0);
//            button.click();
//            webClient.waitForBackgroundJavaScript(1000 * 5);
//
//            indexText = indexPage.asText();
//            logger.info("*** After submission:\n{}", indexText);
//
//            if(indexText.toLowerCase().contains("thanks for"))
//                bot.sendMessage(channel, "Sent ;)");
//            else
//                bot.sendMessage(channel, "Something went wrong, I think they're onto us");
//
//        } catch (Exception e) {
//            bot.sendMessage(channel, "Failed to send request");
//        } finally {
//            webClient.getCookieManager().setCookiesEnabled(false);
//        }

        /* end anti-schroogling countermeasures */

//        Future<Integer> future = executorService.submit(new Callable<Integer>(){
//            @Override
//            public Integer call() throws Exception {
//
//                Integer statusCode = -1;
//                HttpGet get = null;
//                HttpPost post = null;
//                try {
//
//                    get = new HttpGet("http://www.theharveyparty.com/music/");
//                    CloseableHttpResponse getResponse = (CloseableHttpResponse) httpClient.execute(get);
//                    String pageText = EntityUtils.toString(getResponse.getEntity());
//
//                    String token = null;
//                    Pattern tokenPattern = Pattern.compile(".*(token=\\d+).*", Pattern.DOTALL);
//                    Matcher tokenMatcher = tokenPattern.matcher(pageText);
//                    while(tokenMatcher.find()) {
//                        token = tokenMatcher.group(1);
//                    }
//
//                    if(StringUtils.isEmpty(token))
//                        throw new Exception("Could not parse token from page");
//
//                    EntityUtils.consumeQuietly(getResponse.getEntity());
//
//                    StringBuilder requestText = new StringBuilder();
//                    for (int i = 1; i < cmdParts.length; i++) {
//                        if (i != 1)
//                            requestText.append(" ");
//                        requestText.append(cmdParts[i]);
//                    }
//
//                    String requester = "TSDIRC " + sender;
//                    String content = String.format("requester=%s&song=%s&token=%s",
//                            requester,
//                            requestText.toString(),
//                            token);
//
//                    post = new HttpPost("http://www.theharveyparty.com/music/sendSong.php");
//                    post.setEntity(new StringEntity(content));
//                    post.addHeader("Content-type", "application/x-www-form-urlencoded");
//
//                    CloseableHttpResponse postResponse = (CloseableHttpResponse) httpClient.execute(post);
//                    statusCode = postResponse.getStatusLine().getStatusCode();
//                    logger.info("HarveyParty response code: {}", statusCode);
//                    logger.info("HarveyParty response text: {}", EntityUtils.toString(postResponse.getEntity()));
//
//                    EntityUtils.consumeQuietly(postResponse.getEntity());
//
//                } catch (Exception e) {
//                    logger.error("Error sending song request", e);
//                    bot.sendMessage(channel, "Error sending song request");
//                } finally {
//                    if(get != null)
//                        get.releaseConnection();
//                    if(post != null)
//                        post.releaseConnection();
//                }
//
//                return statusCode;
//            }
//        });
//
//        try {
//            Integer status = future.get(10, TimeUnit.SECONDS);
//            if(status/100 == 2)
//                bot.sendMessage(channel, "Sent ;)");
//            else
//                bot.sendMessage(channel, "Failed ;)");
//        } catch (InterruptedException | ExecutionException | TimeoutException e) {
//            bot.sendMessage(channel, "Something ain't right...");
//        }

    }
}
