package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jibble.pircbot.User;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.history.filter.*;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.util.IRCUtil;

import java.util.List;
import java.util.Random;

@Singleton
@Function(initialRegex = "^\\.movie")
public class MovieTitle extends MainFunctionImpl {

    private static final String titleFormat = "%s vs %s: %s";

    private final InjectableMsgFilterStrategyFactory filterFactory;
    private final HistoryBuff historyBuff;
    private final Random random;

    @Inject
    public MovieTitle(Bot bot,
                      HistoryBuff historyBuff,
                      Random random,
                      InjectableMsgFilterStrategyFactory filterFactory) {
        super(bot);
        this.description = "Movie title generator";
        this.usage = "USAGE: .movie";
        this.historyBuff = historyBuff;
        this.random = random;
        this.filterFactory = filterFactory;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String item1 = random.nextBoolean() ?
                standardItems[random.nextInt(standardItems.length)] : getRandomNick(channel);

        String item2 = random.nextBoolean() ?
                standardItems[random.nextInt(standardItems.length)] : getRandomNick(channel);

        String subtitle = null;
        if(random.nextBoolean()) {
            NoCommandsStrategy noCmdStrat = new NoCommandsStrategy();
            filterFactory.injectStrategy(noCmdStrat);
            HistoryBuff.Message chosen = historyBuff.getRandomFilteredMessage(
                    channel,
                    null,
                    MessageFilter
                            .create()
                            .addFilter(noCmdStrat)
                            .addFilter(new NoBotsStrategy())
                            .addFilter(new LengthStrategy(5, 30))
                            .addFilter(new NoURLsStrategy())
            );

            if(chosen != null) {
                String[] words = chosen.text.split("\\s+");
                StringBuilder sb = new StringBuilder();
                for(String w : words) {
                    if(sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(w.substring(0, 1).toUpperCase());
                    if(w.length() > 1) {
                        sb.append(w.substring(1));
                    }
                }
                subtitle = sb.toString();
            }
        }

        if(subtitle == null) {
            subtitle = standardSubtitles[random.nextInt(standardSubtitles.length)];
        }

        bot.sendMessage(channel, String.format(titleFormat, item1, item2, subtitle));
    }

    private String getRandomNick(String channel) {
        List<User> users = bot.getNonBotUsers(channel);
        User user;
        String userNick;
        String scrambled;
        do {
            user = users.get(random.nextInt(users.size()));
            userNick = user.getNick();
            scrambled = IRCUtil.scrambleNick(userNick);

            // if this scrambling didn't do anything (e.g. KP -> KP) discard the user and try another
            if(scrambled.equals(userNick)) {
                users.remove(user);
            }

        } while(userNick.equals(scrambled));

        return scrambled;
    }

    private static final String[] standardItems = {
            "Red Panda",        "Panda",
            "Corgi",            "Scottie",
            "Nike",             "Hermes",
            "Dorj",             "Deej",
            "Memes",            "Omega Meme",
            "Summertime",       "Marty",
            "Duke",             "Minmay",
            "TSDTV",            "Lynn Minmay",
            "Lynn Kaifun",      "Kaifun",
            "Bernie",           "Laird",
            "343 Industries",   "Bungie",
            "TSD",              "Bearcopter",
            "IRC",              "Pusheen",
            "Trump",            "The United States of America",
            "America",          "Millennials",
            "Houston",          "Texas",
            "Halo 5",           "Halo 4",
            "Dubai",            "Japan",
            "VOOT",             "tarehart",
            "Tom Cruise",       "Doge",
            "HBO",              "DBO",
            "Louis Wu",         "The World",
            "The State",        "Board of Education",
            "A.C. Slater",      "Saved by the Bell",
            "Anime",            "TSDTV",
            "TSDBot",           "Roy Fokker",
            "Mee-kun"
    };

    private static final String[] standardSubtitles = {
            "Dawn of Justice",
            "The Doomsday Prophecy",
            "The Doomsday Chronicles",
            "Dawn of Night",
            "Dawn of Darkness",
            "From Dusk til Dawn",
            "Collateral Damage",
            "Top Gun",
            "Under Arrest",
            "Beyond the Horizon",
            "Fully Loaded",
            "Locked and Loaded",
            "Til the End of Time",
            "The End of Time",
            "Gay Lovers",
            "Dereliction of Duty",
            "Days of Thunder",
            "Days of Blunder",
            "The Color of Money",
            "Extreme Prejudice",
            "Origins",
            "Civil War",
            "Time to Die",
            "A Time to Die",
            "Time to Kill",
            "A View to a Kill",
            "Transformation",
            "Booby Trapped",
            "Saved by the Bell",
            "Darkness Falls",
            "We Have a Problem",
            "Enemy of the State",
            "Under Pressure",
            "Last Stand",
            "Escape Clause",
            "Last Hope",
            "High Stakes",
            "Old Trouble in New Mexico",
            "Beat the Odds",
            "Never Back Down",
            "Mystery Files",
            "The Doomsday Files",
            "Fast Times",
            "High Times",
            "Deadly Force",
            "Maximum Impact",
            "Judgment Day",
            "Final Judgment",
            "Last Stand",
            "All or Nothing",
            "Borhters Under Fire",
            "The Time is Now",
            "Overdrive",
            "Let's Defend Happiness"
    };
}
