package org.tsd.tsdbot;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBotLauncher {

    public static void main(String[] args) throws Exception {
        String name = "TSDBot";
        String server = "localhost";
        String channel = args[0];
        if(!channel.startsWith("#")) channel = "#"+channel;

        if (args.length >= 3) {
            server = args[1];
            name = args[2];
        }

        TSDBot bot = new TSDBot(channel, name);
        bot.setVerbose(true);
        bot.connect(server);
        bot.joinChannel(channel);
    }
}
