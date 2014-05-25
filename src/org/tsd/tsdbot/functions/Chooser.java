package org.tsd.tsdbot.functions;

import org.tsd.tsdbot.TSDBot;

import java.util.Random;

/**
 * Created by Joe on 3/1/14.
 */
public class Chooser implements MainFunction {

    private static String[] formats = new String[] {
            "Go with %s",
            "I choose %s"
    };

    @Override
    public void run(String channel, String sender, String text) {
        String[] args = text.split("\\s+",2);
        if(args.length == 2) {
            String[] choices = args[1].split("[\\s]*\\|[\\s]*");
            if(choices.length <= 1) {
                TSDBot.getInstance().sendMessage(channel, "Gimme some choices bro");
                return;
            }

            Random rand = new Random();
            String returnString = String.format(formats[rand.nextInt(formats.length)], choices[rand.nextInt(choices.length)]);

            TSDBot.getInstance().sendMessage(channel, returnString);

        } else {
            TSDBot.getInstance().sendMessage(channel, TSDBot.Command.CHOOSE.getUsage());
        }
    }
}
