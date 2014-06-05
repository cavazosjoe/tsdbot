package org.tsd.tsdbot.functions;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;

import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 5/24/14.
 */
public class Filename implements MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(Filename.class);

    @Override
    public void run(String channel, String sender, String ident, String text) {
        TSDBot bot = TSDBot.getInstance();
        CloseableHttpClient httpClient = bot.getHttpClient();
        HttpGet fnamesGet = null;
        try {

            fnamesGet = new HttpGet("http://teamschoolyd.org/filenames/");
            fnamesGet.setHeader("User-Agent", "Mozilla/4.0");
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response = httpClient.execute(fnamesGet, responseHandler);

            Random rand = new Random();

            Matcher m = Pattern.compile("a href=\"([\\w_]+?\\.\\w{3})\"", Pattern.DOTALL).matcher(response);
            String pfx = "http://www.teamschoolyd.org/filenames/";
            LinkedList<String> all = new LinkedList<>();
            while(m.find()) {
                all.add(m.group(1));
            }
            bot.sendMessage(channel, pfx + all.get(rand.nextInt(all.size())));

        } catch (Exception e) {
            logger.error("filename() error",e);
            bot.blunderCount++;
        } finally {
            if(fnamesGet != null) fnamesGet.releaseConnection();
        }
    }
}
