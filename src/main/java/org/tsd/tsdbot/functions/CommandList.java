package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.module.Function;

@Singleton
@Function(initialRegex = "^\\.cmd$")
public class CommandList extends MainFunctionImpl {

    private String serverUrl;

    @Inject
    public CommandList(TSDBot bot, @Named("serverUrl") String serverUrl) {
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
