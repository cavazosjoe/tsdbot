package org.tsd.tsdbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBotLauncher {

    // TSDBot.jar tsd-test irc.teamschoolyd.org TSDBot [debug]
    public static void main(String[] args) throws Exception {
        String name = "TSDBot";
        String server = "localhost";
        String channel = args[0];
        if(!channel.startsWith("#")) channel = "#"+channel;
        boolean debug = false;

        if (args.length >= 3) {
            server = args[1];
            name = args[2];
        }

        if(args.length >= 4) {
            debug = args[3].equalsIgnoreCase("debug");
        }

        Properties prop = new Properties();
        InputStream fis = TSDBotLauncher.class.getResourceAsStream("/tsdbot.properties");
        prop.load(fis);
        String nickservPass = prop.getProperty("pass");

        TSDBot bot = new TSDBot(name,new String[]{channel},debug);
        bot.setVerbose(false);
        bot.setMessageDelay(25); //25 ms
        bot.connect(server);
        if(nickservPass != null && (!nickservPass.isEmpty()))
            bot.identify(nickservPass);
        bot.joinChannel(channel);
    }
}
