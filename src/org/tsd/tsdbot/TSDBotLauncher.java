package org.tsd.tsdbot;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.List;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBotLauncher {

    public static void main(String[] args) throws Exception {
        String server = "localhost";
        String channel = "#tsd";

        TSDBot bot = new TSDBot();
        bot.setVerbose(true);
        bot.connect(server);
        bot.joinChannel(channel);
    }
}
