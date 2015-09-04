package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.module.Function;

import java.util.Random;

/**
 * Created by Joe on 3/1/14.
 */
@Singleton
@Function(initialRegex = "^\\.choose.*")
public class Chooser extends MainFunctionImpl {

    private Random random;

    @Inject
    public Chooser(Bot bot, Random random) {
        super(bot);
        this.description = "Have the bot choose a random selection for you";
        this.usage = "USAGE: .choose option1 | option2 [ | option3...]";
        this.random = random;
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

            String format = formats[random.nextInt(formats.length)];
            String choice = null;

            if(sender.equalsIgnoreCase("GV") || sender.contains("Vague")) {
                int i=0;
                while(choice == null && i < choices.length) {
                    if(choices[i].length() > 15)
                        choice = fakeChoices[random.nextInt(fakeChoices.length)];
                    i++;
                }
            }

            if(choice == null)
                choice = choices[random.nextInt(choices.length)];

            bot.sendMessage(channel, String.format(format, choice));

        } else {
            bot.sendMessage(channel, usage);
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
            "space magic! xD",              "The TSD Limo",
            "Macross",                      "Johnny Manziel",
            "now now NOW NOW NOW GO GO GO NOW NOW NOW"
    };
}
