package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.module.Function;

import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
@Function(initialRegex = "^\\.(filename|fname)$")
public class Filename extends MainFunctionImpl {

    private static final Logger logger = LoggerFactory.getLogger(Filename.class);

    private static final String filenamesLocation = "http://www.teamschoolyd.org/filenames/";

    private HttpClient httpClient;
    private Random random;

    @Inject
    public Filename(Bot bot, HttpClient httpClient, Random random) {
        super(bot);
        this.description = "Pull a random entry from the TSD Filenames Database";
        this.usage = "USAGE: [ .filename | .fname ]";
        this.httpClient = httpClient;
        this.random = random;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        HttpGet fnamesGet = null;
        try {
            fnamesGet = new HttpGet(filenamesLocation);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response = httpClient.execute(fnamesGet, responseHandler);

            Matcher m = Pattern.compile("a href=\"([\\w_]+?\\.\\w{3})\"", Pattern.DOTALL).matcher(response);
            LinkedList<String> all = new LinkedList<>();
            while(m.find()) {
                all.add(m.group(1));
            }
            bot.sendMessage(channel, filenamesLocation + all.get(random.nextInt(all.size())));

        } catch (Exception e) {
            logger.error("filename() error",e);
            bot.incrementBlunderCnt();
        } finally {
            if(fnamesGet != null) fnamesGet.releaseConnection();
        }
    }

}
