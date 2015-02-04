package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.tsdtv.*;
import org.tsd.tsdbot.tsdtv.model.TSDTVEpisode;
import org.tsd.tsdbot.tsdtv.model.TSDTVShow;

import java.sql.SQLException;
import java.util.Random;

/**
 * Created by Joe on 1/12/2015.
 */
@Singleton
public class TSDTVFunction extends MainFunction {

    private static final Logger logger = LoggerFactory.getLogger(TSDTVFunction.class);

    private TSDTV tsdtv;
    private TSDTVLibrary library;
    private String serverUrl;
    private Random random;

    @Inject
    public TSDTVFunction(TSDBot bot, TSDTV tsdtv, TSDTVLibrary library, @Named("serverUrl") String serverUrl, Random random) {
        super(bot);
        this.description = "The TSDTV Streaming Entertainment Value Service";
        this.usage = "USAGE: .tsdtv [ catalog [<directory>] | play [<movie-name> | <directory> <movie-name>] ]";
        this.tsdtv = tsdtv;
        this.library = library;
        this.serverUrl = serverUrl;
        this.random = random;
    }

    @Override
    protected void run(String channel, String sender, String ident, String text) {
        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length < 2) {
            bot.sendMessage(channel, usage);
            return;
        }

        boolean isOp = bot.getUserFromNick(channel, sender).hasPriv(User.Priv.OP);
        String subCmd = cmdParts[1];

        if(subCmd.equals("catalog")) {
            bot.sendMessage(channel, serverUrl + "/tsdtv");
        } else if(subCmd.equals("replay")) {

            if(cmdParts.length < 3) {
                bot.sendMessage(channel, usage);
                return;
            }

            tsdtv.prepareBlockReplay(channel, cmdParts[2]);

        } else if(subCmd.equals("play")) {

            if(cmdParts.length != 4) {
                bot.sendMessage(channel, "USAGE: .tsdtv play <show> [ <episode> | random ]");
                return;
            }

            try {
                TSDTVShow show = library.getShow(cmdParts[2]);
                TSDTVEpisode episode = (TSDTVConstants.RANDOM_QUERY.equals(cmdParts[3])) ?
                        show.getRandomEpisode(random) : show.getEpisode(cmdParts[3]);
                if(!tsdtv.playFromChat(episode, ident)) {
                    bot.sendMessage(channel, "There is already a stream running. Your show has been enqueued");
                }
            } catch (Exception e) {
                logger.error("Error playing from chat", e);
                bot.sendMessage(channel, "Error: " + e.getMessage());
            }

        } else if(subCmd.equals("kill")) {

            try {
                if (cmdParts.length == 3 && cmdParts[2].equals("all")) { // kill everything
                    if (isOp) {
                        tsdtv.kill(false);
                        bot.sendMessage(channel, "The stream has been nuked");
                    } else {
                        bot.sendMessage(channel, "Only ops can use that");
                    }
                } else { // just kill whatever's playing now
                    if (isOp || tsdtv.authorized(ident)) {
                        tsdtv.kill(true);
                        bot.sendMessage(channel, "The stream has been killed");
                    } else {
                        bot.sendMessage(channel, "You don't have permission to end the current video");
                    }
                }
            } catch (NoStreamRunningException nsre) {
                bot.sendMessage(channel, "There's no stream running");
            }

        } else if(subCmd.equals("pause")) {

            try {
                if (isOp || tsdtv.authorized(ident)) {
                    tsdtv.pause();
                    bot.sendMessage(channel, "The stream has been paused");
                } else {
                    bot.sendMessage(channel, "You don't have permission to pause this stream");
                }
            } catch (NoStreamRunningException nsre) {
                bot.sendMessage(channel, "There's no stream running");
            } catch (IllegalStateException ise) {
                bot.sendMessage(channel, "Error: " + ise.getMessage());
            }

        } else if(subCmd.equals("unpause")) {

            try {
                if (isOp || tsdtv.authorized(ident)) {
                    tsdtv.unpause();
                    bot.sendMessage(channel, "The stream has been unpaused");
                } else {
                    bot.sendMessage(channel, "You don't have permission to unpause this stream");
                }
            } catch (NoStreamRunningException nsre) {
                bot.sendMessage(channel, "There's no stream running");
            } catch (IllegalStateException ise) {
                bot.sendMessage(channel, "Error: " + ise.getMessage());
            }

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
