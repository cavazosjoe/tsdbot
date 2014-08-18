package org.tsd.tsdbot.functions;

import org.jibble.pircbot.User;
import org.tsd.tsdbot.TSDBot;

/**
 * Created by Joe on 5/24/14.
 */
public class ShutItDown extends MainFunction {

    @Override
    public void run(String channel, String sender, String ident, String text) {

        TSDBot bot = TSDBot.getInstance();

        if(!bot.getUserFromNick(channel, sender).hasPriv(User.Priv.SUPEROP)) {
            bot.kick(channel, sender, "Stop that.");
        } else {
            //TODO: change this to actually shut down
            bot.partChannel(channel, "ABORT ABORT ABORT");
        }
    }
}
