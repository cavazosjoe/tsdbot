package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import org.jibble.pircbot.User;
import org.tsd.tsdbot.TSDBot;

import java.util.Date;

/**
 * Created by Joe on 5/24/14.
 */
public abstract class MainFunction {

    @Inject
    protected TSDBot bot;
    private Long cooldownMillis;
    private Date lastUsed;

    public MainFunction(TSDBot bot) {
        this.bot = bot;
    }

    protected MainFunction(int cooldownMinutes) {
        this.cooldownMillis = (long)cooldownMinutes * 60 * 1000;
    }

    @Deprecated // should be injected?
    protected MainFunction() {}

    public void engage(String channel, String sender, String ident, String text) {
        long timeRemaining = getRemainingCooldown(); // millis
        if(timeRemaining <= 0 || bot.getUserFromNick(channel, sender).hasPriv(User.Priv.OP)) {
            run(channel, sender, ident, text);
            lastUsed = new Date();
        } else {
            int minutesLeft = (int)Math.ceil( ((double)timeRemaining) / ((double)(1000 * 60)) ); // necessary?
            bot.sendMessage(channel, "That function will be available in " + minutesLeft + " minute(s)");
        }
    }

    protected abstract void run(String channel, String sender, String ident, String text);

    private long getRemainingCooldown() {
        if(cooldownMillis == null || lastUsed == null) return -1; // no cooldown or hasn't been run yet
        return lastUsed.getTime() + cooldownMillis - System.currentTimeMillis();
    }
}
