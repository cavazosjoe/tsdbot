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
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        bot.sendMessage(channel, "I'm sending you a message with my list of commands, " + sender);
        boolean first = true;
        for(TSDBot.Command command : TSDBot.Command.values()) {
            if(command.getDesc() != null) {
                if(!first) bot.sendMessage(sender, "-----------------------------------------");
                bot.sendMessage(sender, command.getDesc());
                bot.sendMessage(sender, command.getUsage());
                first = false;
            }
        }
    }
}
