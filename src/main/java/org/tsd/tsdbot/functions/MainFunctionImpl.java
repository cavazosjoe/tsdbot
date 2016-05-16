package org.tsd.tsdbot.functions;

import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.module.Function;

public abstract class MainFunctionImpl implements MainFunction {

    protected TSDBot bot;
    protected String description;
    protected String usage;

    protected String listeningRegex;

    public MainFunctionImpl(TSDBot bot) {
        this.bot = bot;
        this.listeningRegex = getClass().getAnnotation(Function.class).initialRegex();
    }

    @Deprecated // should be injected?
    protected MainFunctionImpl() {}

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public String getListeningRegex() {
        return listeningRegex;
    }

    public abstract void run(String channel, String sender, String ident, String text);

}
