package org.tsd.tsdbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.runnable.TSDTVStream;
import sun.reflect.Reflection;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBotLauncher {

    // TSDBot.jar tsd-test irc.teamschoolyd.org TSDBot [debug]
    public static void main(String[] args) throws Exception {

        Logger log = LoggerFactory.getLogger(TSDBotLauncher.class);

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

        log.info("channel={}, server={} , name={} , debug={}", channel, server, name, debug);

        Properties prop = new Properties();
        InputStream fis = TSDBotLauncher.class.getResourceAsStream("/tsdbot.properties");
        prop.load(fis);
        String nickservPass = prop.getProperty("nickserv.pass");

        TSDBot bot = new TSDBot(name,new String[]{channel},debug);
        bot.setVerbose(false);
        bot.setMessageDelay(10); //10 ms
        bot.connect(server);
        if(nickservPass != null && (!nickservPass.isEmpty()))
            bot.identify(nickservPass);
        bot.joinChannel(channel);

        log.info("TSDBot loaded successfully. Beginning conquest...");

//        TSDTVStream stream = new TSDTVStream();
//        ExecutorService threadPool = Executors.newFixedThreadPool(1);
//        threadPool.submit(stream);
    }
}
