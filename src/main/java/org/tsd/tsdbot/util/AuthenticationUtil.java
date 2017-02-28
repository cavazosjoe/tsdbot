package org.tsd.tsdbot.util;

import com.google.inject.Inject;
import org.jibble.pircbot.User;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.module.BotOwner;
import org.tsd.tsdbot.module.MainChannel;

public class AuthenticationUtil {

    private final String mainChannel;
    private final String botOwner;

    @Inject
    public AuthenticationUtil(@BotOwner String botOwner, @MainChannel String mainChannel) {
        this.mainChannel = mainChannel;
        this.botOwner = botOwner;
    }

    public boolean userIsOp(User user) {
        return user.getNick().equals(botOwner) || user.hasPriv(User.Priv.OP);
    }

    public boolean userIsOwner(String nick) {
        return nick.equals(botOwner);
    }

    public boolean userHasGlobalPriv(TSDBot bot, String nick, User.Priv priv) {
        return userHasPrivInChannel(bot, nick, mainChannel, priv);
    }

    public boolean userHasPrivInChannel(TSDBot bot, String nick, String channel, User.Priv priv) {
        User u = bot.getUserFromNick(channel, nick);
        return u != null && (userIsOwner(nick) || u.hasPriv(priv));
    }

}
