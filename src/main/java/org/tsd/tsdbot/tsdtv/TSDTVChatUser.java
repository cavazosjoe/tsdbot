package org.tsd.tsdbot.tsdtv;

import org.jibble.pircbot.User;

public class TSDTVChatUser extends TSDTVUser {

    private User user;

    public TSDTVChatUser(User user) {
        this.user = user;
    }

    @Override
    public boolean isOp() {
        return user.hasPriv(User.Priv.OP);
    }

    @Override
    public String getId() {
        return user.getNick();
    }
}
