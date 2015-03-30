package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jibble.pircbot.User;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.Function;

/**
 * Created by Joe on 5/24/14.
 */
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
        if(!bot.userHasGlobalPriv(sender, User.Priv.SUPEROP)) {
            bot.kick(channel, sender, "Stop that.");
        } else {
            //TODO: change this to actually shut down
            bot.partChannel(channel, "ABORT ABORT ABORT");
        }
    }

}
