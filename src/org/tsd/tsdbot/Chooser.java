package org.tsd.tsdbot;

import java.util.Random;

/**
 * Created by Joe on 3/1/14.
 */
public class Chooser {

    public static String choose(String message) {
        String[] args = message.split("\\s+",2);
        if(args.length == 2) {
            String[] choices = args[1].split("[\\s]*\\|[\\s]*");
            if(choices.length <= 1) return "Gimme some choices bro";

            Random rand = new Random();

            return String.format(formats[rand.nextInt(formats.length)],
                    choices[rand.nextInt(choices.length)]);

        }
        return TSDBot.Command.CHOOSE.getUsage();
    }

    private static String[] formats = new String[] {
            "%s. Make it happen",
            "I choose %s",
            "%s, and soon!",
            "%s"
    };
}
