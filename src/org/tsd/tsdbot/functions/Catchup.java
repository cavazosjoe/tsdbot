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

    private int cooldownMinutes = 10;

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
        while(history.size() > 0 && chosen.size() < 5) {
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
            "Survivor: TSDIRC"
    };

    private static enum DramaStyle {
        exclamation {
            @Override
            public String apply(String s) {
                //hackish way to get rid of periods/ellipses, too lazy for regex
                if(s.endsWith(".")) s = s.substring(0, s.length()-1);
                else if(s.endsWith("...")) s = s.substring(0,s.length()-3);

                s = s + "!!";
                return s;
            }
        },
        question {
            @Override
            public String apply(String s) {
                //hackish way to get rid of periods/ellipses, too lazy for regex
                if(s.endsWith(".")) s = s.substring(0, s.length()-1);
                else if(s.endsWith("...")) s = s.substring(0,s.length()-3);

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
    }
}
