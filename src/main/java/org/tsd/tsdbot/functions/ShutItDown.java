package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.util.AuthenticationUtil;

@Singleton
@Function(initialRegex = "^\\.SHUT_IT_DOWN$")
public class ShutItDown extends MainFunctionImpl {

    private final AuthenticationUtil authenticationUtil;

    @Inject
    public ShutItDown(TSDBot bot, AuthenticationUtil authenticationUtil) {
        super(bot);
        this.authenticationUtil = authenticationUtil;
        this.description = "SHUT IT DOWN (owner only)";
        this.usage = "USAGE: SHUT IT DOWN";
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        if(authenticationUtil.userIsOwner(sender)) {
            bot.partChannel(channel, "ABORT ABORT ABORT");
            bot.shutdownNow();
        } else {
            bot.sendMessage(channel, ":^)");
        }
    }
}
