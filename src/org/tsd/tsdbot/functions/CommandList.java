package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.Command;
import org.tsd.tsdbot.TSDBot;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class CommandList extends MainFunction {

    @Inject
    public CommandList(TSDBot bot) {
        super(bot);
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        bot.sendMessage(channel, "http://irc.teamschoolyd.org/commands.txt");
    }
}
