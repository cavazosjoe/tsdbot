package org.tsd.tsdbot.functions;

import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.history.MessageFilter;
import org.tsd.tsdbot.history.MessageFilterStrategy;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Joe on 5/24/14.
 */
public class Deej extends MainFunction {

    @Override
    public void run(String channel, String sender, String ident, String text) {
        Random rand = new Random();
        TSDBot bot = TSDBot.getInstance();
        HistoryBuff historyBuff = HistoryBuff.getInstance();
        HistoryBuff.Message chosen = historyBuff.getRandomFilteredMessage(
                channel,
                null,
                MessageFilter.create().addFilter(new MessageFilterStrategy.NoCommandsStrategy())
        );

        if(chosen != null) {
            // return the deej-formatted selected message
            bot.sendMessage(channel, String.format(formats[rand.nextInt(formats.length)], chosen));
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
