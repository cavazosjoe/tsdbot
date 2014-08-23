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
public class Catchup extends MainFunction {

    private static final int dramaCount = 4;

    public Catchup() {
        super(10);
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        Random rand = new Random();
        TSDBot bot = TSDBot.getInstance();
        HistoryBuff historyBuff = HistoryBuff.getInstance();
        List<HistoryBuff.Message> history = historyBuff.getMessagesByChannel(channel, null);

        // keep sampling random messages, discarding ones that are commands
        LinkedList<HistoryBuff.Message> chosen = new LinkedList<>();
        while(history.size() > 0 && chosen.size() < dramaCount) {
            HistoryBuff.Message msg = history.get(rand.nextInt(history.size()));
            if(TSDBot.Command.fromString(msg.text).size() == 0 && msg.text.length() < 80)
                chosen.add(msg);
            history.remove(msg);
        }

        if(!chosen.isEmpty()) {

            bot.sendMessage(channel,
                    String.format(formats[rand.nextInt(formats.length)], showNames[rand.nextInt(showNames.length)]));

            HistoryBuff.Message m;
            while(chosen.size() > 1) {
                m = chosen.pop();
                bot.sendMessage(channel, "<" + m.sender + "> " + DramaStyle.getRandomDrama(m.text));
            }

            // end on an ominous note
            m = chosen.pop();
            bot.sendMessage(channel, "<" + m.sender + "> " + DramaStyle.ellipses.apply(m.text));

            bot.sendMessage(channel, "Tonight's episode: \"" + episodeNames[rand.nextInt(episodeNames.length)] + "\"");
        }
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
            "#tsd",
            "Team Schooly D",
            "Team Schooly D IRC",
            "TSD: IRC",
            "Team Schooly D: Internet Relay Chat",
            "Schooly and the Funky Bunch",
            "Fiasco & Blunder: Halo Cops",
            "TSD High",
            "TSDU",
            "TSD: Miami",
            "Survivor: TSDIRC",
            "TSD: The College Years",
            "Fast Times at TSD High",
            "Slappy Days",
            "T.S.D.I.R.C."
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
            "tarehart Goes to College",
            "The Graduation",
            "Video Games",
            "Tex and the Five Magics",
            "The Bonkening",
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

        public static String getRandomDrama(String s) {
            Random rand = new Random();
            DramaStyle[] dramas = new DramaStyle[]{exclamation, question, bold, caps}; // no ellipses
            return dramas[rand.nextInt(dramas.length)].apply(s);
        }

        private static String stripPeriods(String s) {
            if(s.endsWith("...")) s = s.substring(0,s.length()-3);
            else if(s.endsWith("..")) s = s.substring(0, s.length()-2);
            else if(s.endsWith(".")) s = s.substring(0, s.length()-1);
            return s;
        }
    }
}
