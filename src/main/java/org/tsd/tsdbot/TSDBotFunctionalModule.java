package org.tsd.tsdbot;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.tsd.tsdbot.functions.*;
import org.tsd.tsdbot.notifications.*;
import org.tsd.tsdbot.stats.GvStats;
import org.tsd.tsdbot.stats.HustleStats;
import org.tsd.tsdbot.stats.Stats;
import org.tsd.tsdbot.stats.SystemStats;

/**
 * Created by Joe on 3/28/2015.
 */
public class TSDBotFunctionalModule extends AbstractModule {
    @Override
    protected void configure() {
        bindStats();
        bindNotifiers();
        bindFunctions();
    }

    private void bindStats() {
        Multibinder<Stats> statsBinder = Multibinder.newSetBinder(binder(), Stats.class);
        statsBinder.addBinding().to(HustleStats.class);
        statsBinder.addBinding().to(SystemStats.class);
        statsBinder.addBinding().to(GvStats.class);
    }

    private void bindFunctions() {
        Multibinder<MainFunction> functionBinder = Multibinder.newSetBinder(binder(), MainFunction.class);
        functionBinder.addBinding().to(Archivist.class);
        functionBinder.addBinding().to(BlunderCount.class);
        functionBinder.addBinding().to(Chooser.class);
        functionBinder.addBinding().to(CommandList.class);
        functionBinder.addBinding().to(Deej.class);
        functionBinder.addBinding().to(Filename.class);
        functionBinder.addBinding().to(FourChan.class);
        functionBinder.addBinding().to(GeeVee.class);
        functionBinder.addBinding().to(OmniPost.class);
        functionBinder.addBinding().to(org.tsd.tsdbot.functions.Twitter.class);
        functionBinder.addBinding().to(Recap.class);
        functionBinder.addBinding().to(Replace.class);
        functionBinder.addBinding().to(Sanic.class);
        functionBinder.addBinding().to(ScareQuote.class);
        functionBinder.addBinding().to(ShutItDown.class);
        functionBinder.addBinding().to(StrawPollFunction.class);
        functionBinder.addBinding().to(TomCruise.class);
        functionBinder.addBinding().to(Wod.class);
        functionBinder.addBinding().to(SillyZackDark.class);
        functionBinder.addBinding().to(Printout.class);
        functionBinder.addBinding().to(XboxLive.class);
        functionBinder.addBinding().to(Hustle.class);
        functionBinder.addBinding().to(TSDTVFunction.class);
        functionBinder.addBinding().to(Dorj.class);
        functionBinder.addBinding().to(OmniDB.class);
        functionBinder.addBinding().to(DboFireteamFunction.class);
    }

    private void bindNotifiers() {
        Multibinder<NotificationManager> notificationBinder = Multibinder.newSetBinder(binder(), NotificationManager.class);
        notificationBinder.addBinding().to(TwitterManager.class);
        notificationBinder.addBinding().to(HboForumManager.class);
        notificationBinder.addBinding().to(HboNewsManager.class);
        notificationBinder.addBinding().to(DboForumManager.class);
        notificationBinder.addBinding().to(DboNewsManager.class);
        notificationBinder.addBinding().to(DboFireteamManager.class);
    }
}
