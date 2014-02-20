package org.tsd.tsdbot;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBotLauncher {

    public static void main(String[] args) throws Exception {
        String server = "localhost";
        String channel = args[0];
        if(!channel.startsWith("#")) channel = "#"+channel;

        TSDBot bot = new TSDBot(channel);
        bot.setVerbose(true);
        bot.connect(server);
        bot.joinChannel(channel);
    }
}
