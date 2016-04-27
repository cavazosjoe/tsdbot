package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.module.Function;

@Singleton
@Function(initialRegex = "^\\.SHUT_IT_DOWN$")
public class ShutItDown extends MainFunctionImpl {

    @Inject
    public ShutItDown(Bot bot) {
        super(bot);
        this.description = "SHUT IT DOWN (owner only)";
        this.usage = "USAGE: SHUT IT DOWN";
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        if(bot.userIsOwner(sender)) {
            bot.partChannel(channel, "ABORT ABORT ABORT");
            bot.shutdownNow();
        } else {
            bot.sendMessage(channel, ":^)");
        }
    }

}
