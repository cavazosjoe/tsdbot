package org.tsd.tsdbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

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

        Properties prop = new Properties();
        InputStream fis = TSDBotLauncher.class.getResourceAsStream("/tsdbot.properties");
        prop.load(fis);

        TSDBot bot = new TSDBot(name,new String[]{channel});
        bot.setVerbose(false);
        bot.connect(server);
        bot.identify(prop.getProperty("pass"));
        bot.joinChannel(channel);
    }
}
