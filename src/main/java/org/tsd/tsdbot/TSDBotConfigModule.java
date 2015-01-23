package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.hyperic.sigar.Sigar;
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
import org.tsd.tsdbot.runnable.DorjThread;
import org.tsd.tsdbot.runnable.InjectableIRCThreadFactory;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.scheduled.InjectableJobFactory;
import org.tsd.tsdbot.stats.GvStats;
import org.tsd.tsdbot.stats.HustleStats;
import org.tsd.tsdbot.stats.Stats;
import org.tsd.tsdbot.stats.SystemStats;
import org.tsd.tsdbot.tsdtv.InjectableStreamFactory;
import org.tsd.tsdbot.tsdtv.TSDTV;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.io.*;
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

        /**
         * Need to load some platform-specific libraries for SIGAR system info API.
         * Because these libs are included in the JAR file, we need to do some hackery
         * to load them individually: extract each file to a tmp directory, then load
         * that file into the system
         */
        try {
            log.info("Loading SIGAR libraries...");
            String tmpPath = System.getProperty("java.io.tmpdir");

            // the output directory where the libs will be extracted
            File sigDir = new File(tmpPath + "/sigar/");
            if(sigDir.exists())
                sigDir.delete();
            sigDir.mkdir();

            InputStream in = null;
            File fileOut = null;
            OutputStream out = null;

            // iterate over each entry in the manifest, locate file in JAR, and extract to tmp folder
            try(BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("sigarlist.txt")))) {
                for(String line; (line = br.readLine()) != null; ) try {
                    in = getClass().getClassLoader().getResourceAsStream("sigar/" + line);
                    fileOut = new File(tmpPath + "/sigar/" + line);
                    out = FileUtils.openOutputStream(fileOut);
                    IOUtils.copy(in, out);
                    in.close();
                    out.close();
                    System.load(fileOut.toString());
                    log.info("Successfully loaded {}", fileOut.toString());
                } catch (UnsatisfiedLinkError e) {
                    log.warn("Failed to load {}, skipping...", fileOut.toString());
                }
            }

            bind(Sigar.class).toInstance(new Sigar());
            log.info("SIGAR successfully initialized");
        } catch (Exception e) {
            log.error("Error loading SIGAR libraries", e);
        }

        String hostname = properties.getProperty("server.hostname");
        bind(String.class).annotatedWith(ServerHostname.class)
                .toInstance(hostname);

        int port = Integer.parseInt(properties.getProperty("server.port"));
        bind(Integer.class).annotatedWith(ServerPort.class)
                .toInstance(port);

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://").append(hostname);
        if(port != 80)
            urlBuilder.append(":").append(port);
        bind(String.class).annotatedWith(Names.named("serverUrl"))
                .toInstance(urlBuilder.toString());

        bind(Stage.class).toInstance(stage);

        bind(TSDBot.class).toInstance(bot);

        bind(Properties.class).toInstance(properties);

        bind(HistoryBuff.class).asEagerSingleton();
        bind(Archivist.class).asEagerSingleton();

        bind(ThreadManager.class).toInstance(new ThreadManager(10));
        bind(InjectableIRCThreadFactory.class).asEagerSingleton();

        bind(InjectableStreamFactory.class).toInstance(new InjectableStreamFactory());

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
                .setConnectionReuseStrategy(new NoConnectionReuseStrategy())
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
                ffmpegExec,
                "-re",
                "-y",
                "-i",       "%s",           // %s -> path to file, to be formatted later

                "-override_ffserver",       // passing stream parameters via this command, not in ffserver.conf
                "-c:v",     "libx264",      // video codec (always use libx264)
                "-r",       "20",           // framerate
                "-b:v",     "1200k",        // video bitrate
                "-maxrate", "1700k",        // max bitrate
                "-bufsize", "2560k",        // buffer size (used when averaging bitrate)
                "-preset",  "superfast",    // processing speed (ultrafast = least CPU, worst vid quality)
                "-profile:v","baseline",    // something for Apple devices or VLC or something
                "-pix_fmt", "yuv420p",      // something for Apple devices or VLC or something
                "-flags:v", "+global_header",//something that seems to work
                "-vf",      "%s",           // %s -> video filter, to be formatted later (used for subs)
                                            // see TSDTV.play()

                "-c:a",     "aac",          // vv audio junk, who cares lol vv
                "-b:a",     "128k",
                "-ar",      "44100",
                "-strict",  "experimental",
                "-flags:a", "+global_header",

                "http://localhost:8090/feed1.ffm"
        };
        String ffmpeg = StringUtils.join(ffmpegParts, " ");
        bind(String.class)
                .annotatedWith(Names.named("ffmpeg"))
                .toInstance(ffmpeg);

        bind(TSDTV.class).asEagerSingleton();

        bindStats();
        bindFunctions();
        bindNotifiers();

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

    }

    private void bindNotifiers() {
        Multibinder<NotificationManager> notificationBinder = Multibinder.newSetBinder(binder(), NotificationManager.class);
        notificationBinder.addBinding().to(TwitterManager.class);
        notificationBinder.addBinding().to(HboForumManager.class);
        notificationBinder.addBinding().to(HboNewsManager.class);
        notificationBinder.addBinding().to(DboForumManager.class);
        notificationBinder.addBinding().to(DboNewsManager.class);
    }

}
