package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Command;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.history.HistoryBuff;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Joe on 4/5/14.
 */
@Singleton
public class GeeVee extends MainFunction {

    private static Logger logger = LoggerFactory.getLogger(GeeVee.class);

    private HistoryBuff historyBuff;
    private Random random;

    @Inject
    public GeeVee(TSDBot bot, HistoryBuff historyBuff, Random random) {
        super(bot);
        this.historyBuff = historyBuff;
        this.random = random;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length == 1) {

            HistoryBuff.Message randomMsg = historyBuff.getRandomFilteredMessage(channel, null, null);
            if(randomMsg != null) {
                bot.sendMessage(channel, "<" + randomMsg.sender + "> " + randomMsg.text);
                bot.sendMessage(channel, getRandomGvResponse());
            }

        } else if(cmdParts.length == 2) {

            if(!cmdParts[1].equals("pls")) {
                bot.sendMessage(channel, Command.GV.getUsage());
            } else {
                String[] gvAliases = {"general","gv"};
                List<HistoryBuff.Message> gvLines = new LinkedList<>();
                for(String alias : gvAliases) {
                    if(!gvLines.isEmpty()) break;
                    gvLines.addAll(historyBuff.getMessagesByChannel(channel, alias));
                }

                if(gvLines.size() == 0) return;

                String runOnSentence = null;
                int i=0;
                while(runOnSentence == null && i < gvLines.size()) {
                    runOnSentence = GeeVee.breakUpSentence(gvLines.get(i).text);
                    i++;
                }

                if(runOnSentence == null)
                    bot.sendMessage(channel, "I don't see anything wrong here, quit being mean");
                else
                    bot.sendMessage(channel, runOnSentence);
            }
        }
    }

    public String getRandomGvResponse() {
        return gvResponses[random.nextInt(gvResponses.length)];
    }

    // This is a long sentence, it could be longer, but this is just a, you know, sentence
    // This is a long sentence. It could be longer. But this is just a. You know. Sentence
    public static String breakUpSentence(String sentence) {
        try {
            String[] clauses = sentence.split(", ");
            if(clauses.length > 3) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for(String s : clauses) {
                    if(!first) sb.append(" ");
                    String firstChar = s.substring(0,1);
                    sb.append(firstChar.toUpperCase()).append(s.substring(1));
                    if(!s.endsWith(".")) sb.append(".");
                    first = false;
                }
                return sb.toString();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static String[] gvResponses = new String[]{
            "I don't think you understand, but whatever",
            "Why?",
            "I don't think that's correct",
            "Why would you think that?",
            "I'm pretty sure I heard about that, but maybe not",
            "That doesn't really sound like something I'd like",
            "I'm not interested, sorry",
            "I don't really care, sorry",
            "Where did you see that?",
            "Where did you read that?",
            "Where did you hear that?",
            "I'm pretty sure science says no, but you can disagree I guess, maybe",
            "Why do you care?",
            "Why would you care?",
            "That's something I've always found interesting",
            "That's something I never found interesting, sorry",
            "I don't understand. Why?",
            "This isn't really something I want to discuss, sorry",
            "This isn't something I want to discuss, sorry",
            "I thought we agreed that was wrong?",
            "Why would you bring this up now?",
            "That's not very interesting",
            "Why am I supposed to care about that?",
            "I'd ask you to elaborate but I don't really care, sorry",
            "I'd ask you to elaborate but I'm not interested, sorry",
            "I know I'm late but I read this and I don't care",
            "I'm trying to imagine how this could be lamer and I'm having trouble, maybe if someone's mom was involved?",
            "Oh wow, let me get my heart medication",
            "Yawn"
    };
}
