package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class CommandList extends MainFunction {

    @Inject
    public CommandList(TSDBot bot) {
        super(bot);
        this.description = "Have the bot send you a list of commands";
        this.usage = "USAGE: .cmd";
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        bot.sendMessage(channel, "http://irc.teamschoolyd.org/commands.txt");
    }

    @Override
    public String getRegex() {
        return "^\\.cmd$";
    }
}
