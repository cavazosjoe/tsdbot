package org.tsd.tsdbot.functions;

import org.tsd.tsdbot.HistoryBuff;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.util.IRCUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Joe on 5/24/14.
 */
public class ScareQuote extends MainFunction {

    //TODO: put more than 5 minutes of effort into this and stop trying to "fix" it while drunk

    @Override
    public void run(String channel, String sender, String ident, String text) {

        Random rand = new Random();
        TSDBot bot = TSDBot.getInstance();
        HistoryBuff historyBuff = HistoryBuff.getInstance();
        List<HistoryBuff.Message> history = historyBuff.getMessagesByChannel(channel, null);

        // keep sampling random messages, discarding ones that are commands
        HistoryBuff.Message chosen = null;
        while(chosen == null && (!history.isEmpty())) {
            HistoryBuff.Message msg = history.get(rand.nextInt(history.size()));
            String[] w = msg.text.split("\\s+");
            if(TSDBot.Command.fromString(msg.text).size() == 0 && msg.text.length() < 100 && w.length > 2)
                chosen = msg;
            history.remove(msg);
        }

        if(chosen != null) {
            String[] words = chosen.text.split("\\s+");
            int r = rand.nextInt(words.length);
            String scary = words[r];
            if(!scary.startsWith("\""))
                scary = "\""+scary;
            if(!scary.endsWith("\""))
                scary = scary+"\"";

            StringBuilder result = new StringBuilder();
            int i = 0;
            for(String w : words) {
                if(i > 0) result.append(" ");
                if(i == r) result.append(scary);
                else result.append(w);
                i++;
            }

            bot.sendMessage(channel, "<" + IRCUtil.scrambleNick(chosen.sender) + "> " + result.toString());
        }
    }

}
