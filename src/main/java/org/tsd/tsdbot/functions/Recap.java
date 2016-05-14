package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.history.filter.InjectableMsgFilterStrategyFactory;
import org.tsd.tsdbot.history.filter.LengthStrategy;
import org.tsd.tsdbot.history.filter.MessageFilter;
import org.tsd.tsdbot.history.filter.NoCommandsStrategy;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.util.IRCUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

@Singleton
@Function(initialRegex = "^\\.recap")
public class Recap extends MainFunctionImpl {

    private static final int dramaCount = 4;

    private InjectableMsgFilterStrategyFactory filterFactory;
    private HistoryBuff historyBuff;
    private Random random;

    @Inject
    public Recap(Bot bot, HistoryBuff historyBuff, Random random, InjectableMsgFilterStrategyFactory filterFactory) {
        super(bot);
        this.description = "Recap function. Get a dramatic recap of recent chat history";
        this.usage = "USAGE: .recap [ minutes (integer) ]";
        this.historyBuff = historyBuff;
        this.random = random;
        this.filterFactory = filterFactory;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        NoCommandsStrategy noCmdStrat = new NoCommandsStrategy();
        filterFactory.injectStrategy(noCmdStrat);
        LinkedList<HistoryBuff.Message> chosen = historyBuff.getRandomFilteredMessages(
                channel,
                null,
                dramaCount,
                MessageFilter.create()
                        .addFilter(noCmdStrat)
                        .addFilter(new LengthStrategy(0, 80))
        );

        if(!chosen.isEmpty()) {

            // use a dictionary to keep fudged nicks consistent through the catchup
            HashMap<String, String> scrambleDict = new HashMap<>();

            bot.sendMessage(channel,
                    String.format(formats[random.nextInt(formats.length)], showNames[random.nextInt(showNames.length)]));

            HistoryBuff.Message m;
            while(chosen.size() > 1) {
                m = chosen.pop();
                bot.sendMessage(channel, "<" + getScrambledNick(m.sender, scrambleDict) + "> " + DramaStyle.getRandomDrama(m.text, random));
            }

            // end on an ominous note
            m = chosen.pop();
            bot.sendMessage(channel, "<" + getScrambledNick(m.sender, scrambleDict) + "> " + DramaStyle.ellipses.apply(m.text));

            bot.sendMessage(channel, "Tonight's episode: \"" + episodeNames[random.nextInt(episodeNames.length)] + "\"");
        }
    }

    private String getScrambledNick(String nick, HashMap<String, String> dict) {
        if(!dict.containsKey(nick)) {
            dict.put(nick, IRCUtil.scrambleNick(nick));
        }
        return dict.get(nick);
    }

    private static final String[] formats = new String[] {
            "Last time, on a very special \"%s\":",
            "Last week, on a very special \"%s\":",
            "Last time on \"%s\":",
            "Last week on \"%s\":",
            "On last week's \"%s\":",
            "Previously on \"%s\":"
    };

    private static final String[] showNames = new String[] {
            "#tsd",                                 "Team Schooly D",
            "Team Schooly D IRC",                   "TSD: IRC",
            "Team Schooly D: Internet Relay Chat",  "Schooly and the Funky Bunch",
            "Fiasco & Blunder: Halo Cops",          "TSD High",
            "TSDU",                                 "TSD: Miami",
            "Survivor: TSDIRC",                     "TSD: The College Years",
            "Fast Times at TSD High",               "Slappy Days",
            "T.S.D.I.R.C.",                         "Hajime no Kanbo",
            "Tips & Tricks: Professional Rusemen",  "Real Housewives of Bellevue"
    };

    private static final String[] episodeNames = new String[] {
            "GV's Wild Ride",
            "Crash! The Server's Down for Maintenance?!",
            "The Mystery of DeeJ",
            "Schooly Joins the Army",
            "Hickory Dickory... Dead",
            "Paddy's Big Secret",
            "Little kanbo, Big Adventure!",
            "KP DOA",
            "ZackDark in America",
            "The Red Menace",
            "The Downward Spiral",
            "The Argument",
            "Tarehart Goes to College",
            "The Graduation",
            "Video Games",
            "Tex and the Five Magics",
            "The Bonkening",
            "Paddy's Big Goodbye",
            "The KP Caper",
            "Planes, Banes, and Batmobiles",
            "The Laird Problem",
            "Corgidome",
            "Nart Goes to Bed",
            "TDSpiral and the Intervention That Saved Christmas",
            "The Double DorJ",
            "The Splash Bash",
            "A Day without a ZackDark",
            "A Fistful of Clonkers",
            "For a Few Bonks More",
            "BaneKin's Ruse",
            "Paddy's Gambit",
            "Dr. DeeJ and Mr. DorJ",
            "Nart-kun, I'm Sorry",
            "The Swole Toll",
            "If Ever a Whiff There Was",
            "BoneKin Dies",
            "Everyone Dies",
            "The Case of the Missing tarehart",
            "A Good Ban Is Hard to Find",
            "Everyone Has Fun Playing the Master Chief Collection",
            "Star Macross'd Lovers",
            "Rumble in the Tumblr",
            "Minmay's Gambit",
            "Grillin' with Bernie",
            "Bar in the Pocket: Tex and the Hidden Flask Technique",
            "Take Flight, Gundam!",
            "Dr. Strangedeej, or: How I Learned to Stop Worrying and Love the Dorj",
            "Dr. GV, PhD, although I guess if he was a medical doctor he wouldn't have a PhD? Or maybe they can, " +
                    "I don't know. I know he'd be called \"Dr.\" though. I think they should make that clearer, like " +
                    "in the dictionary or wherever they spell things out like that. But I guess it wouldn't be an English " +
                    "thing it'd be a medical licensing and terminology thing? Uuuuuuugggggghhhh it's already so late " +
                    "and I was supposed to go to bed 23 minutes ago but then this came up and uuuggggghhhhh >_>"
    };

    private static enum DramaStyle {
        exclamation {
            @Override
            public String apply(String s) {
                //hackish way to get rid of periods/ellipses, too lazy for regex
                s = stripPeriods(s);
                s = s + "!!";
                return s;
            }
        },
        question {
            @Override
            public String apply(String s) {
                //hackish way to get rid of periods/ellipses, too lazy for regex
                s = stripPeriods(s);
                s = s + "??";
                return s;
            }
        },
        bold {
            @Override
            public String apply(String s) {
                return IRCUtil.BOLD_CHAR + s;
            }
        },
        caps {
            @Override
            public String apply(String s) {
                return s.toUpperCase();
            }
        },
        ellipses {
            @Override
            public String apply(String s) {
                if(!s.endsWith("...")) {
                    if(s.endsWith("?") || s.endsWith("!")) s = s.substring(0, s.length()-1);
                    return s + "...";
                }
                return s; // already ends with ellipses...
            }
        };

        public abstract String apply(String s);

        public static String getRandomDrama(String s, Random random) {
            DramaStyle[] dramas = new DramaStyle[]{exclamation, question, bold, caps}; // no ellipses
            return dramas[random.nextInt(dramas.length)].apply(s);
        }

        private static String stripPeriods(String s) {
            return s.replaceAll("\\.+$","");
        }
    }
}
