package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.module.Function;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
@Function(initialRegex = "^\\.cmd$")
public class CommandList extends MainFunctionImpl {

    private String serverUrl;

    @Inject
    public CommandList(Bot bot, @Named("serverUrl") String serverUrl) {
        super(bot);
        this.description = "Have the bot send you a list of commands";
        this.usage = "USAGE: .cmd";
        this.serverUrl = serverUrl;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        bot.sendMessage(channel, serverUrl+"/commands.txt");
    }

}
