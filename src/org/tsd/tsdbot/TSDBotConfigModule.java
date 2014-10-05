package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.DBConnectionString;
import org.tsd.tsdbot.functions.*;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.notifications.*;
import org.tsd.tsdbot.scheduled.InjectableJobFactory;
import org.tsd.tsdbot.tsdtv.InjectableStreamFactory;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;
import java.util.Random;

/**
 * Created by Joe on 9/19/2014.
 */
public class TSDBotConfigModule extends AbstractModule {

    Logger log = LoggerFactory.getLogger(TSDBotConfigModule.class);

    private TSDBot bot;
    private Stage stage;
    private Properties properties;

    public TSDBotConfigModule(TSDBot bot, Properties properties, Stage stage) {
        this.bot = bot;
        this.properties = properties;
        this.stage = stage;
    }

    @Override
    protected void configure() {

        bind(Stage.class).toInstance(stage);

        bind(TSDBot.class).toInstance(bot);

        bind(Properties.class).toInstance(properties);

        bind(HistoryBuff.class).asEagerSingleton();
        bind(Archivist.class).asEagerSingleton();

        PoolingHttpClientConnectionManager poolingManager = new PoolingHttpClientConnectionManager();
        poolingManager.setMaxTotal(100);
        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                if(i >= 5) return false; // don't try more than 5 times
                return e instanceof NoHttpResponseException;
            }
        };
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(poolingManager)
                .setRetryHandler(retryHandler)
                .build();
        bind(PoolingHttpClientConnectionManager.class).toInstance(poolingManager);
        bind(HttpClient.class).toInstance(httpClient);

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getCookieManager().setCookiesEnabled(true);
        bind(WebClient.class).toInstance(webClient);

        bind(Twitter.class).toInstance(TwitterFactory.getSingleton());

        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            bind(Scheduler.class).toInstance(scheduler);
            bind(JobFactory.class).to(InjectableJobFactory.class);
        } catch (SchedulerException e) {
            log.error("ERROR WHILE BUILDING SCHEDULER", e);
            bot.broadcast("Error while building scheduler: " + e.getMessage());
        }

        bind(String.class)
                .annotatedWith(DBConnectionString.class)
                .toInstance(properties.getProperty("db.connstring"));
        bind(Connection.class).toProvider(DBConnectionProvider.class);

        bind(Random.class).toInstance(new Random());

        requestInjection(new InjectableStreamFactory());
        String ffmpegExec = properties.getProperty("tsdtv.ffmpeg");
        String[] ffmpegParts = new String[]{
                "nice",     "-n","8",
                ffmpegExec,
                "-re",
                "-y",
                "-i",       "%s", // %s -> path to file, to be formatted later
                "http://localhost:8090/feed1.ffm"
        };
        String ffmpeg = StringUtils.join(ffmpegParts, " ");
        bind(String.class)
                .annotatedWith(Names.named("ffmpeg"))
                .toInstance(ffmpeg);

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
        functionBinder.addBinding().to(StrawPoll.class);
        functionBinder.addBinding().to(TomCruise.class);
        functionBinder.addBinding().to(Wod.class);
        functionBinder.addBinding().to(SillyZackDark.class);
        if(stage.equals(Stage.production))
            functionBinder.addBinding().to(TSDTV.class);

        Multibinder<NotificationManager> notificationBinder = Multibinder.newSetBinder(binder(), NotificationManager.class);
        notificationBinder.addBinding().to(TwitterManager.class);
        notificationBinder.addBinding().to(HboForumManager.class);
        notificationBinder.addBinding().to(HboNewsManager.class);
        notificationBinder.addBinding().to(DboForumManager.class);
        notificationBinder.addBinding().to(DboNewsManager.class);

    }
}
