package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.ThreadType;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.notifications.TwitterManager;
import org.tsd.tsdbot.runnable.DorjThread;
import org.tsd.tsdbot.runnable.InjectableIRCThreadFactory;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.util.IRCUtil;

@Singleton
@Function(initialRegex = "^\\.dorj.*")
public class Dorj extends MainFunctionImpl {

    private static final Logger logger = LoggerFactory.getLogger(Dorj.class);

    private InjectableIRCThreadFactory threadFactory;
    private ThreadManager threadManager;
    private TwitterManager twitterManager;
    private long lastDorjId = -1;

    @Inject
    public Dorj(TSDBot bot, ThreadManager threadManager,
                InjectableIRCThreadFactory threadFactory, TwitterManager twitterManager) {
        super(bot);
        this.description = "Dorj: use teamwork to summon the legendary Double Dorj";
        this.usage = "USAGE: .dorj";
        this.threadManager = threadManager;
        this.threadFactory = threadFactory;
        this.twitterManager = twitterManager;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length == 1) {

            DorjThread existingThread = (DorjThread) threadManager.getIrcThread(ThreadType.DORJ, channel);
            if (existingThread == null) try {
                if(IRCUtil.detectBot(sender)) {
                    bot.sendMessage(channel, "A robot cannot control another robot");
                    return;
                }
                existingThread = threadFactory.newDorjThread(channel, ident);
                threadManager.addThread(existingThread);
            } catch (Exception e) {
                logger.error("Error building Dorj thread", e);
                bot.sendMessage(channel, "Error building Dorj thread: " + e.getMessage());
            }

        } else if(cmdParts[1].equals("rollback")) {

            if(!bot.userIsOwner(sender)) {
                bot.sendMessage(channel, "Only my master can change the course of history");
                return;
            }

            if(lastDorjId < 0) {
                bot.sendMessage(channel, "No Dorj to rollback");
            } else {
                bot.sendMessage(channel, "(a soft wind caresses the cheeks of those who bore witness as the sands of time recede and the Dorj is undone)");
                twitterManager.delete(channel, lastDorjId);
                lastDorjId = -1;
            }
        }

    }

    public void setSuccessfulDorj(long tweetId) {
        this.lastDorjId = tweetId;
    }

}
