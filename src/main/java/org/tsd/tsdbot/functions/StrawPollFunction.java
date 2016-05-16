package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.ThreadType;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.runnable.InjectableIRCThreadFactory;
import org.tsd.tsdbot.runnable.StrawPoll;
import org.tsd.tsdbot.runnable.ThreadManager;

@Singleton
@Function(initialRegex = "^\\.poll.*")
public class StrawPollFunction extends MainFunctionImpl {

    private InjectableIRCThreadFactory threadFactory;
    private ThreadManager threadManager;

    @Inject
    public StrawPollFunction(TSDBot bot, ThreadManager threadManager, InjectableIRCThreadFactory threadFactory) {
        super(bot);
        this.description = "Strawpoll: propose a question and choices for the chat to vote on";
        this.usage = "USAGE: .poll <question> ; <duration (integer)> ; choice 1 ; choice 2 [; choice 3 ...]";
        this.threadManager = threadManager;
        this.threadFactory = threadFactory;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        // this is an operation on a running poll, disregard
        if(StrawPoll.StrawPollOperation.fromString(text) != null)
            return;

        String[] cmdParts = text.split(";");

        org.tsd.tsdbot.runnable.StrawPoll currentPoll =
                (org.tsd.tsdbot.runnable.StrawPoll) threadManager.getIrcThread(ThreadType.STRAWPOLL, channel);

        if(currentPoll != null) {
            bot.sendMessage(channel, "There is already a poll running. It will end in "
                    + (currentPoll.getRemainingTime() / (60 * 1000)) + " minute(s)");
            return;
        }

        if(cmdParts.length < 4) {
            bot.sendMessage(channel, usage);
            return;
        }

        String question = cmdParts[0].substring(cmdParts[0].indexOf(" ") + 1).trim(); // .poll This is a question
        int minutes = Integer.parseInt(cmdParts[1].trim());
        String[] choices = new String[cmdParts.length-2];
        for(int i=0 ; i < choices.length ; i++) {
            choices[i] = cmdParts[i+2].trim();
        }

        try {
            currentPoll = threadFactory.newStrawPoll(channel, sender, question, minutes, choices);
            threadManager.addThread(currentPoll);
        } catch (Exception e) {
            bot.sendMessage(channel, e.getMessage());
            bot.incrementBlunderCnt();
        }
    }

}
