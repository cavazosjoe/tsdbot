package org.tsd.tsdbot.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static Logger log = LoggerFactory.getLogger(TSDBotFunctionalModule.class);

    @Override
    protected void configure() {
        log.info("Binding stats...");
        bindStats();
        log.info("Binding notifiers...");
        bindNotifiers();
        log.info("Binding functions...");
        bindFunctions();
        log.info("TSDBotFunctionalModule.configure() successful");
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
            log.info("- Binding function: {}...", clazz);
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
