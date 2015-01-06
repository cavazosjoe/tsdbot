package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.history.HistoryBuff;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Properties;

/**
 * Created by Joe on 1/3/2015.
 */
@Singleton
public class Hustle extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Hustle.class);

    private static final int period = 5; // every [period]th message will be sent for hustle analysis
    private static final DecimalFormat df = new DecimalFormat("##0.0#");
    private static final String fmt = "Current Hustle/Hate ratio for %s: %s";

    private HttpClient httpClient;
    private String apiKey;

    CircularFifoBuffer huffleBustle = new CircularFifoBuffer(50);

    private int msgCnt = 0;

    @Inject
    public Hustle(TSDBot bot, HttpClient httpClient, Properties properties) {
        super(bot);
        this.httpClient = httpClient;
        this.apiKey = properties.getProperty("mashape.apiKey");
    }

    @Override
    protected void run(String channel, String sender, String ident, String text) {
        double hustle = 1;
        double hate = 1;
        for(Object o : huffleBustle) {
            HustleItem item = (HustleItem)o;
            double itemScore = (item.confidence/100) * text.length();
            switch (item.sentiment) {
                case Positive: hustle += itemScore; break;
                case Negative: hate += itemScore; break;
                case Neutral: {
                    hustle += itemScore;
                    hate += itemScore;
                    break;
                }
            }
        }
        double ratio = hustle / hate;
        logger.info("H/H: {}/{} = {}", hustle, hate, ratio);
        bot.sendMessage(channel, String.format(fmt, channel, df.format(ratio)));
    }

    public void process(String channel, String text) {
        if(msgCnt++ < period)
            return;
        else msgCnt = 0;

        logger.info("Sending latest message for sentiment analysis...");

        HttpPost post = null;
        try {
            post = new HttpPost("https://community-sentiment.p.mashape.com/text/");
            post.addHeader("X-Mashape-Key", apiKey);
            post.addHeader("Content-Type", "application/x-www-form-urlencoded");

            LinkedList<NameValuePair> params = new LinkedList<>();
            params.add(new BasicNameValuePair("txt", text));
            post.setEntity(new UrlEncodedFormEntity(params));
            CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(post);
            String responseString = EntityUtils.toString(response.getEntity());

            JSONObject json = new JSONObject(responseString);
            for(String key : json.keySet()) {
                if(key.equals("result")) {
                    HustleItem item = new HustleItem(json.getJSONObject(key).getString("sentiment"), json.getJSONObject(key).getString("confidence"));
                    huffleBustle.add(item);
                    logger.info("Analysis result: {} (Confidence {})", item.sentiment, item.confidence);
                }
            }

            EntityUtils.consumeQuietly(response.getEntity());

        } catch (Exception e) {
            logger.error("Error retrieving text sentiment", e);
            bot.sendMessage(channel, "(Error calculating hustle quotient, please check logs)");
        } finally {
            if(post != null)
                post.releaseConnection();
        }
    }

    enum Sentiment {
        Positive,
        Negative,
        Neutral;

        public static Sentiment fromString(String s) {
            for(Sentiment sentiment : values()) {
                if(sentiment.toString().toLowerCase().equals(s.toLowerCase()))
                    return sentiment;
            }
            return null;
        }
    }

    class HustleItem {
        public Sentiment sentiment;
        public double confidence;

        HustleItem(String sentiment, String confidence) {
            this.sentiment = Sentiment.fromString(sentiment);
            this.confidence = Double.parseDouble(confidence);
        }
    }
}
