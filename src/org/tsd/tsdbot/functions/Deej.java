package org.tsd.tsdbot.functions;

import org.tsd.tsdbot.HistoryBuff;
import org.tsd.tsdbot.TSDBot;

import java.util.List;
import java.util.Random;

/**
 * Created by Joe on 5/24/14.
 */
public class Deej implements MainFunction {

    @Override
    public void run(String channel, String sender, String text) {
        Random rand = new Random();
        TSDBot bot = TSDBot.getInstance();
        HistoryBuff historyBuff = HistoryBuff.getInstance();
        List<HistoryBuff.Message> history = historyBuff.getMessagesByChannel(channel, null);

        if(history.size() > 0) {
            String returnString = String.format(formats[rand.nextInt(formats.length)], history.get(rand.nextInt(history.size())).text);
            bot.sendMessage(channel, returnString);
        }
    }

    private static final String[] formats = new String[] {
            "Fear not, Guardians: %s",
            "But be wary, Guardians of our city: %s",
            "The scribes of our city, stewards of the lost knowledge from our Golden Age, have uncovered a mysterious tome " +
                    "whose pages are all empty but for one mysterious line: \"%s\"",
            "Rejoice, Guardians! %s",
            "The spirits and specters from our bygone era of prosperity remind us: %s"
    };
}
