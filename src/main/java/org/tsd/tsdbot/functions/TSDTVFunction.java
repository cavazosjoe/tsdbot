package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.tsdtv.ShowInfo;
import org.tsd.tsdbot.tsdtv.ShowNotFoundException;
import org.tsd.tsdbot.tsdtv.TSDTV;

import java.io.File;
import java.sql.SQLException;

/**
 * Created by Joe on 1/12/2015.
 */
@Singleton
public class TSDTVFunction extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(TSDTVFunction.class);

    private TSDTV tsdtv;
    private String serverUrl;

    @Inject
    public TSDTVFunction(TSDBot bot, TSDTV tsdtv, @Named("serverUrl") String serverUrl) {
        super(bot);
        this.description = "The TSDTV Streaming Entertainment Value Service";
        this.usage = "USAGE: .tsdtv [ catalog [<directory>] | play [<movie-name> | <directory> <movie-name>] ]";
        this.tsdtv = tsdtv;
        this.serverUrl = serverUrl;
    }

    @Override
    protected void run(String channel, String sender, String ident, String text) {
        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length < 2) {
            bot.sendMessage(channel, usage);
            return;
        }

        String subCmd = cmdParts[1];

        if(subCmd.equals("catalog")) {

            try {
                if(cmdParts.length > 2) {
                    String showDirString = cmdParts[2].replaceAll("/","");
                    File showDir = tsdtv.getShowDir(showDirString);
                    bot.sendMessage(channel, serverUrl + "/tsdtv/catalog?show=" + showDir.getName());
                } else {
                    bot.sendMessage(channel, serverUrl + "/tsdtv/catalog");
                }
            } catch (ShowNotFoundException snfe) {
                bot.sendMessage(channel, "Error retrieving catalog: " + snfe.getMessage());
            }

        } else if(subCmd.equals("replay")) {

            if(cmdParts.length < 3) {
                bot.sendMessage(channel, usage);
                return;
            }

            tsdtv.prepareBlockReplay(channel, cmdParts[2]);

        } else if(subCmd.equals("play")) {

            String subdir = null;
            String query;
            if(cmdParts.length > 3) {
                subdir = cmdParts[2].replaceAll("/","");
                query = cmdParts[3].replaceAll("/", "");
            } else {
                query = cmdParts[2].replaceAll("/","");
            }

            try {
                tsdtv.prepareOnDemand(channel, subdir, query);
            } catch (Exception e) {
                bot.sendMessage(channel, "Error: " + e.getMessage());
            }

        } else if(subCmd.equals("kill")) {

            if(!bot.getUserFromNick(channel, sender).hasPriv(User.Priv.OP)) {
                bot.sendMessage(channel, "Only ops can use that");
                return;
            }
            tsdtv.kill();
            bot.sendMessage(channel, "The stream has been killed");

        } else if(subCmd.equals("reload")) {

            if(!bot.getUserFromNick(channel, sender).hasPriv(User.Priv.OP)) {
                bot.sendMessage(channel, "Only ops can use that");
                return;
            }

            try{
                tsdtv.initDB();
                logger.info("");
            } catch (SQLException e) {
                logger.error("Error re-initializing TSDTV DB", e);
                bot.sendMessage(channel, "Error re-initializing TSDTV DB");
            }
            tsdtv.buildSchedule();
            bot.sendMessage(channel, "The schedule has been reloaded");

        } else if(subCmd.equals("schedule")) {

            if(cmdParts.length > 2 && cmdParts[2].equalsIgnoreCase("all"))
                tsdtv.printSchedule(channel, false);
            else
                tsdtv.printSchedule(channel, true);

        } else if(subCmd.equals("viewers")) {

            int count = tsdtv.getViewerCount();
            String msg;
            switch (count) {
                case -1: msg = "An error occurred getting the viewer count"; break;
                case 1: msg = "There is 1 viewer watching the stream"; break;
                default: msg = "There are " + count + " viewers watching the stream"; break;
            }
            if(!tsdtv.isRunning()) {
                msg += ". But there isn't a stream running";
            }
            bot.sendMessage(channel, msg);

        } else if(subCmd.equals("current")) {

            // .tsdtv current ippo
            if(cmdParts.length > 2) {
                try {
                    ShowInfo result = tsdtv.getPrevAndNextEpisodeNums(cmdParts[2]);
                    StringBuilder sb = new StringBuilder();
                    sb.append("The next episode of ")
                            .append(result.name)
                            .append(" will be ")
                            .append(result.nextEpisode)
                            .append(". The previously watched episode was ")
                            .append(result.previousEpisode)
                            .append(".");
                    bot.sendMessage(channel, sb.toString());
                } catch (Exception e) {
                    bot.sendMessage(channel, "Error: " + e.getMessage());
                }
            } else {
                bot.sendMessage(channel, usage);
            }
        } else if(subCmd.equals("links")) {
            bot.sendMessage(channel, tsdtv.getLinks(true));
        }
    }

    @Override
    public String getRegex() {
        return "^\\.tsdtv.*";
    }
}
