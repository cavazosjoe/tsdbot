package org.tsd.tsdbot.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.reflections.Reflections;
import org.tsd.tsdbot.functions.MainFunction;
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
        Reflections reflections = new Reflections("org.tsd.tsdbot.functions");
        for(Class clazz : reflections.getTypesAnnotatedWith(Function.class)) {
            functionBinder.addBinding().to(clazz);
        }
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
