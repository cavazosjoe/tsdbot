package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;

import java.util.Random;

/**
 * Created by Joe on 3/1/14.
 */
@Singleton
public class Chooser extends MainFunction {

    @Inject
    public Chooser(TSDBot bot) {
        super(bot);
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        String[] args = text.split("\\s+",2);
        if(args.length == 2) {
            String[] choices = args[1].split("[\\s]*\\|[\\s]*");
            if(choices.length <= 1) {
                bot.sendMessage(channel, "Gimme some choices bro");
                return;
            }

            Random rand = new Random();
            String format = formats[rand.nextInt(formats.length)];
            String choice = null;

            if(sender.equalsIgnoreCase("GV") || sender.contains("Vague")) {
                for(String c : choices) {
                    if(c.length() > 15) {
                        choice = fakeChoices[rand.nextInt(choices.length)];
                        break;
                    }
                }
            } else {
                choice = choices[rand.nextInt(choices.length)];
            }

            bot.sendMessage(channel, String.format(format, choice));

        } else {
            bot.sendMessage(channel, TSDBot.Command.CHOOSE.getUsage());
        }
    }

    private static String[] formats = new String[] {
            "Go with %s",
            "I choose %s",
            "Rejoice, Guardian! The holders of the ancient ways have chosen %s",
            "I don't always choose, but when I do, I choose %s",
            "Always bet on %s"

    };

    private static String[] fakeChoices = new String[] {
            "yes",                          "no",
            "maybe",                        "the one on the left",
            "clean the VCR, it's dirty",    "graduate school",
            "Temjin",                       "Cypher",
            "30",                           "31",
            "ERROR",                        "NULL",
            "Deadlifts",                    "Bench Press",
            "Julio Jones",                  "most of Al Sharpton",
            "three monitors",               "Cooler Ranch",
            "your own blood",               "don't",
            "DON'T",                        "PANIC",
            "Nart",                         "message her, what's the worst that could happen?",
            "believe it",                   "chocolate",
            "Homeward Bound",               "Homeward Bound 2",
            "space magic! xD"
    };
}
