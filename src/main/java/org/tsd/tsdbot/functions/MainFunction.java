package org.tsd.tsdbot.functions;

import org.jibble.pircbot.User;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.TSDBot;

import java.util.Date;

/**
 * Created by Joe on 5/24/14.
 */
public abstract class MainFunction {

    protected Bot bot;
    protected String description;
    protected String usage;
    private Long cooldownMillis;
    private Date lastUsed;

    public MainFunction(Bot bot) {
        this.bot = bot;
    }

    protected MainFunction(TSDBot bot, int cooldownMinutes) {
        this.bot = bot;
        this.cooldownMillis = (long)cooldownMinutes * 60 * 1000;
    }

    @Deprecated // should be injected?
    protected MainFunction() {}

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public void engage(String channel, String sender, String ident, String text) {
        long timeRemaining = getRemainingCooldown(); // millis
        if(timeRemaining <= 0 || bot.userHasPrivInChannel(sender, channel, User.Priv.OP)) {
            run(channel, sender, ident, text);
            lastUsed = new Date();
        } else {
            int minutesLeft = (int)Math.ceil( ((double)timeRemaining) / ((double)(1000 * 60)) ); // necessary?
            bot.sendMessage(channel, "That function will be available in " + minutesLeft + " minute(s)");
        }
    }

    private long getRemainingCooldown() {
        if(cooldownMillis == null || lastUsed == null) return -1; // no cooldown or hasn't been run yet
        return lastUsed.getTime() + cooldownMillis - System.currentTimeMillis();
    }

    protected abstract void run(String channel, String sender, String ident, String text);
    public abstract String getRegex();
}
