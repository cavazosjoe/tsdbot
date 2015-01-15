package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.history.*;

import java.util.Random;
import java.util.Set;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class Deej extends MainFunction {

    private InjectableMsgFilterStrategyFactory filterFactory;
    private HistoryBuff historyBuff;
    private Random random;

    @Inject
    public Deej(TSDBot bot, HistoryBuff historyBuff, Random random, InjectableMsgFilterStrategyFactory filterFactory) {
        super(bot);
        this.description = "DeeJ utility. Picks a random line from the channel history and makes it all dramatic and shit";
        this.usage = "USAGE: .deej";
        this.historyBuff = historyBuff;
        this.random = random;
        this.filterFactory = filterFactory;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        NoCommandsStrategy noCmdStrat = new NoCommandsStrategy();
        filterFactory.injectStrategy(noCmdStrat);
        HistoryBuff.Message chosen = historyBuff.getRandomFilteredMessage(
                channel,
                null,
                MessageFilter.create().addFilter(noCmdStrat)
        );

        if(chosen != null) {
            // return the deej-formatted selected message
            bot.sendMessage(channel, String.format(formats[random.nextInt(formats.length)], chosen.text));
        }
    }

    @Override
    public String getRegex() {
        return "^\\.deej$";
    }

    private static final String[] formats = new String[] {
            "Fear not, Guardians: %s",
            "But be wary, Guardians of our city: %s",
            "The scribes of our city, stewards of the lost knowledge from our Golden Age, have uncovered a " +
                    "mysterious tome whose pages are all empty but for one mysterious line: \"%s\"",
            "Rejoice, Guardians! %s",
            "The spirits and specters from our bygone era of prosperity remind us: %s",
            "A message appears written in the amber skies above Earth's last city, at once a harbinger of caution " +
                    "and hope for all whose light shines bright against the darkness: \"%s\""
    };
}
