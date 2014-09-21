package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class StrawPoll extends MainFunction {

    @Inject
    public StrawPoll(TSDBot bot) {
        super(bot);
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split(";");
        TSDBot.Command cmd = TSDBot.Command.STRAWPOLL;

        String[] splitOnWhitespace = cmdParts[0].split("\\s+");
        if(splitOnWhitespace.length > 1 && cmd.threadCmd(splitOnWhitespace[1])) return;

        org.tsd.tsdbot.runnable.StrawPoll currentPoll =
                (org.tsd.tsdbot.runnable.StrawPoll) bot.getThreadManager().getIrcThread(TSDBot.ThreadType.STRAWPOLL, channel);

        if(currentPoll != null) {
            bot.sendMessage(channel, "There is already a poll running. It will end in " + (currentPoll.getRemainingTime() / (60 * 1000)) + " minute(s)");
            return;
        }

        if(cmdParts.length < 4) {
            bot.sendMessage(channel, cmd.getUsage());
            return;
        }

        String question = cmdParts[0].substring(cmdParts[0].indexOf(" ") + 1).trim(); // .poll This is a question
        int minutes = Integer.parseInt(cmdParts[1].trim());
        String[] choices = new String[cmdParts.length-2];
        for(int i=0 ; i < choices.length ; i++) {
            choices[i] = cmdParts[i+2].trim();
        }

        try {
            currentPoll = new org.tsd.tsdbot.runnable.StrawPoll(
                    bot,
                    channel,
                    sender,
                    question,
                    minutes,
                    choices
            );
            bot.getThreadManager().addThread(currentPoll);
        } catch (Exception e) {
            bot.sendMessage(channel, e.getMessage());
            bot.blunderCount++;
        }
    }
}
