package org.tsd.tsdbot;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBotLauncher {

    public static void main(String[] args) throws Exception {
        String server = null;
        String channel = null;
        if(args.length == 1) {
            server = "localhost";
            channel = args[0];
        } else if(args.length == 2) {
            server = args[0];
            channel = args[1];
        } else {
            throw new Exception("usage: TSDBot.jar <server> <channel>");
        }

        if(!channel.startsWith("#")) channel = "#"+channel;

        TSDBot bot = new TSDBot(channel);
        bot.setVerbose(true);
        bot.connect(server);
        bot.joinChannel(channel);
    }
}
