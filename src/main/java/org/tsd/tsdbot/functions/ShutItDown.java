package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jibble.pircbot.User;
import org.tsd.tsdbot.TSDBot;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class ShutItDown extends MainFunction {

    @Inject
    public ShutItDown(TSDBot bot) {
        super(bot);
        this.description = "SHUT IT DOWN (owner only)";
        this.usage = "USAGE: SHUT IT DOWN";
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        if(!bot.getUserFromNick(channel, sender).hasPriv(User.Priv.SUPEROP)) {
            bot.kick(channel, sender, "Stop that.");
        } else {
            //TODO: change this to actually shut down
            bot.partChannel(channel, "ABORT ABORT ABORT");
        }
    }

    @Override
    public String getRegex() {
        return "^\\.SHUT_IT_DOWN$";
    }
}
