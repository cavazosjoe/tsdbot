package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;

import java.util.HashSet;

/**
 * Created by Joe on 10/2/2014.
 */
@Singleton
public class SillyZackDark extends MainFunction {

    private static final String LEFT_GUY = "o/";
    private static final String RIGHT_GUY = "\\o";
    private static final int NEEDED_HELPERS = 3;

    private HashSet<String> helpers = new HashSet<>(); // idents
    private String required = null;
    private String banMask = null;

    @Inject
    public SillyZackDark(TSDBot bot) {
        super(bot);
    }

    @Override
    protected void run(String channel, String sender, String ident, String text) {
        if(ident.equalsIgnoreCase("zack") || ident.equalsIgnoreCase("zackdark") || ident.equalsIgnoreCase("zd")) {
            // zackdark typed this
            if(required == null) { // zackdark has triggered the schroogle
                // require the complementary guy to unschroogle him
                required = (text.contains(LEFT_GUY)) ? RIGHT_GUY : LEFT_GUY;
                banMask = "*!" + ident + "@*";
                bot.ban(channel, banMask);
                bot.sendMessage(channel,
                        "** ZackDark has requested a high five from the channel! I have banned him for his insolence! "
                        + "He CAN be unbanned, but only if " + NEEDED_HELPERS + " help him out by replying "
                        + "\"" + required + "\". Will you leave him hanging again?"
                        );
            }
        } else {
            // someone else typed this
            if(required != null && text.contains(required)) { // someone helping out zd
                helpers.add(ident);
                if(helpers.size() >= NEEDED_HELPERS) { // he's free!
                    bot.sendMessage(channel, "Curses!");
                    bot.unBan(channel, banMask);
                    helpers.clear();
                    required = null;
                    banMask = null;
                }
            }
        }
    }
}
