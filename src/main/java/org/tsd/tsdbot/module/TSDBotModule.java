package org.tsd.tsdbot.module;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.tsd.tsdbot.*;
import org.tsd.tsdbot.config.TSDBotConfiguration;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.DBConnectionString;
import org.tsd.tsdbot.database.JdbcConnectionProvider;
import org.tsd.tsdbot.functions.Archivist;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.runnable.InjectableIRCThreadFactory;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.scheduled.InjectableJobFactory;
import org.tsd.tsdbot.tsdtv.InjectableStreamFactory;
import org.tsd.tsdbot.tsdtv.TSDTV;
import org.tsd.tsdbot.tsdtv.TSDTVFileProcessor;
import org.tsd.tsdbot.tsdtv.TSDTVLibrary;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.io.*;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Joe on 9/19/2014.
 */
public class TSDBotModule extends AbstractModule {

    Logger log = LoggerFactory.getLogger(TSDBotModule.class);

    private TSDBot bot;
    private TSDBotConfiguration configuration;

    public TSDBotModule(TSDBot bot, TSDBotConfiguration configuration) {
        this.bot = bot;
        this.configuration = configuration;
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

        String hostname = configuration.jetty.hostname;
        bind(String.class).annotatedWith(ServerHostname.class)
                .toInstance(hostname);

        int port = configuration.jetty.port;
        bind(Integer.class).annotatedWith(ServerPort.class)
                .toInstance(port);

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://").append(hostname);
        // I have an iptables entry that directs requests on the port specified in properties to port 80
        // uncomment if you don't
        //if(port != 80)
        //    urlBuilder.append(":").append(port);
        String serverUrl = urlBuilder.toString();
        bind(String.class).annotatedWith(Names.named("serverUrl"))
                .toInstance(serverUrl);

        bind(Stage.class).toInstance(configuration.connection.stage);

        bind(Bot.class).toInstance(bot);

        bind(TSDBotConfiguration.class).toInstance(configuration);

        bind(String.class).annotatedWith(MainChannel.class)
                .toInstance(configuration.connection.mainChannel);

        bind(List.class).annotatedWith(AuxChannels.class)
                .toInstance(configuration.connection.auxChannels);

        bind(List.class).annotatedWith(AllChannels.class)
                .toInstance(configuration.connection.getAllChannels());

        bind(Map.class).annotatedWith(NotifierChannels.class)
                .toInstance(configuration.connection.notifiers);

        bind(String.class).annotatedWith(Names.named("mashapeKey"))
                .toInstance(configuration.mashapeKey);

        bind(PrintoutLibrary.class).asEagerSingleton();

        bind(HistoryBuff.class).asEagerSingleton();
        bind(Archivist.class).asEagerSingleton();

        bind(ThreadManager.class).toInstance(new ThreadManager(10));
        bind(InjectableIRCThreadFactory.class).asEagerSingleton();

        bind(InjectableStreamFactory.class).toInstance(new InjectableStreamFactory());

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        bind(ExecutorService.class).toInstance(executorService);

        final PoolingHttpClientConnectionManager poolingManager = new PoolingHttpClientConnectionManager();
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

        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_24);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.getCookieManager().setCookiesEnabled(false);
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
                .toInstance(configuration.database);

        bind(Connection.class).toProvider(DBConnectionProvider.class);
        bind(JdbcConnectionSource.class).toProvider(JdbcConnectionProvider.class);

        bind(Random.class).toInstance(new Random());

        // path to ffmpeg executable
        bind(String.class)
                .annotatedWith(Names.named("ffmpegExec"))
                .toInstance(configuration.tsdtv.ffmpegExec);

        // arguments to ffmpeg commands
        bind(String.class)
                .annotatedWith(Names.named("ffmpegArgs"))
                .toInstance(configuration.tsdtv.ffmpegArgs);

        // output of ffmpeg commands
        bind(String.class)
                .annotatedWith(Names.named("ffmpegOut"))
                .toInstance(configuration.tsdtv.ffmpegOut);

        // direct link to TSDTV stream (to be opened in video players)
        bind(String.class)
                .annotatedWith(Names.named("tsdtvDirect"))
                .toInstance(configuration.tsdtv.directLink);

        // video format used by web player
        bind(String.class)
                .annotatedWith(Names.named("videoFmt"))
                .toInstance(configuration.tsdtv.videoFmt);

        bind(File.class)
                .annotatedWith(Names.named("tsdtvLibrary"))
                .toInstance(new File(configuration.tsdtv.catalog));
        bind(File.class)
                .annotatedWith(Names.named("tsdtvRaws"))
                .toInstance(new File(configuration.tsdtv.raws));
        bind(TSDTVLibrary.class).asEagerSingleton();
        bind(TSDTVFileProcessor.class).asEagerSingleton();

        GoogleAuthHolder googleAuthHolder = new GoogleAuthHolder(configuration.google);
        GoogleCredential googleCredential = new GoogleCredential.Builder()
                .setTransport(new NetHttpTransport())
                .setJsonFactory(new JacksonFactory())
                .setClientSecrets(googleAuthHolder.getClientId(), googleAuthHolder.getClientSecret()).build();
        googleCredential.setRefreshToken(googleAuthHolder.getRefreshToken());

        YouTube youTube = new YouTube.Builder(
                googleCredential.getTransport(),
                googleCredential.getJsonFactory(),
                googleCredential)
                .setApplicationName(googleAuthHolder.getAppId())
                .build();

        bind(YouTube.class).toInstance(youTube);

        bind(TSDTV.class).asEagerSingleton();

        final boolean shutdown = false;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!shutdown) {
                        synchronized (this) {
                            wait(1000 * 30);
                            // Close expired connections
                            poolingManager.closeExpiredConnections();
                            // Optionally, close connections
                            // that have been idle longer than 30 sec
                            poolingManager.closeIdleConnections(30, TimeUnit.SECONDS);
                        }
                    }
                } catch (InterruptedException ex) {
                    // terminate
                    log.warn("Idle connection monitor terminated");
                }
            }
        });

    }

}
