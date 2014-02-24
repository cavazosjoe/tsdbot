package org.tsd.tsdbot;

import java.io.File;

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

        TSDBot bot = new TSDBot(name,new String[]{channel});
        bot.setVerbose(false);
        bot.connect(server);

        File pwFile = new File(System.getProperty("user.dir") + "/pw.txt");
        bot.identify("cashmoneyrecords");
        bot.joinChannel(channel);
    }
}
