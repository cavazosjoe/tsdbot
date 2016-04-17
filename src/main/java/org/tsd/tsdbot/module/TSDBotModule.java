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
import org.apache.commons.lang3.StringUtils;
import org.hyperic.sigar.Sigar;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.*;
import org.tsd.tsdbot.config.GoogleConfig;
import org.tsd.tsdbot.config.TSDBotConfiguration;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.DBConnectionString;
import org.tsd.tsdbot.database.JdbcConnectionProvider;
import org.tsd.tsdbot.functions.Archivist;
import org.tsd.tsdbot.haloapi.MetadataCache;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.runnable.InjectableIRCThreadFactory;
import org.tsd.tsdbot.runnable.ThreadManager;
import org.tsd.tsdbot.scheduled.InjectableJobFactory;
import org.tsd.tsdbot.tsdtv.InjectableStreamFactory;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.io.*;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TSDBotModule extends AbstractModule {

    Logger log = LoggerFactory.getLogger(TSDBotModule.class);

    private final TSDBot bot;
    private final TSDBotConfiguration configuration;

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
        log.info("Bound hostname: {}", hostname);

        int port = configuration.jetty.port;
        bind(Integer.class).annotatedWith(ServerPort.class)
                .toInstance(port);
        log.info("Bound port: {}", port);

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://").append(hostname);
        // I have an iptables entry that directs requests on the port specified in properties to port 80
        // uncomment if you don't
//        if(port != 80)
//            urlBuilder.append(":").append(port);
        String serverUrl = urlBuilder.toString();
        bind(String.class).annotatedWith(Names.named("serverUrl"))
                .toInstance(serverUrl);
        log.info("Bound server URL: {}", serverUrl);

        bind(Stage.class).toInstance(configuration.connection.stage);
        log.info("Bound stage: {}", configuration.connection.stage);

        bind(Bot.class).toInstance(bot);
        log.info("** TSDBot bound successfully **");

        bind(TSDBotConfiguration.class).toInstance(configuration);
        log.info("Bound config");

        bind(String.class).annotatedWith(BotOwner.class)
                .toInstance(configuration.owner);

        bind(String.class).annotatedWith(MainChannel.class)
                .toInstance(configuration.connection.mainChannel);
        log.info("Bound main channel: {}", configuration.connection.mainChannel);

        bind(List.class).annotatedWith(AuxChannels.class)
                .toInstance(configuration.connection.auxChannels);
        log.info("Bound aux channels: {}", StringUtils.join(configuration.connection.auxChannels, ","));

        bind(List.class).annotatedWith(AllChannels.class)
                .toInstance(configuration.connection.getAllChannels());
        log.info("Bound all channels: {}", StringUtils.join(configuration.connection.getAllChannels(), ","));

        bind(Map.class).annotatedWith(NotifierChannels.class)
                .toInstance(configuration.connection.notifiers);
        log.info("Bound notifier channels: {}", configuration.connection.notifiers.toString());

        bind(List.class).annotatedWith(TSDTVChannels.class)
                .toInstance(configuration.connection.tsdtvChannels);
        log.info("Bound TSDTV channels: {}", StringUtils.join(configuration.connection.tsdtvChannels, ","));

        bind(List.class).annotatedWith(TSDFMChannels.class)
                .toInstance(configuration.connection.tsdfmChannels);
        log.info("Bound TSDFM channels: {}", StringUtils.join(configuration.connection.tsdfmChannels, ","));

        bind(String.class).annotatedWith(Names.named("mashapeKey"))
                .toInstance(configuration.mashapeKey);
        log.info("Bound mashape key: {}", configuration.mashapeKey);

        bind(String.class).annotatedWith(Names.named("voiceRssApiKey")).toInstance(configuration.voiceRssApiKey);
        log.info("Bound VoiceRSS key: {}", configuration.voiceRssApiKey);

        log.info("Binding filename library...");
        bind(FilenameLibrary.class).asEagerSingleton();

        log.info("Binding printout library...");
        bind(PrintoutLibrary.class).asEagerSingleton();

        log.info("Binding recap library...");
        bind(RecapLibrary.class).asEagerSingleton();

        log.info("Binding history buff...");
        bind(HistoryBuff.class).asEagerSingleton();

        log.info("Binding archivist...");
        bind(Archivist.class).asEagerSingleton();

        log.info("Binding thread manager...");
        bind(ThreadManager.class).toInstance(new ThreadManager(10));

        log.info("Binding injectable thread factory...");
        bind(InjectableIRCThreadFactory.class).asEagerSingleton();

        log.info("Binding injectable stream factory...");
        bind(InjectableStreamFactory.class).toInstance(new InjectableStreamFactory());

        log.info("Binding executor service...");
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        bind(ExecutorService.class).toInstance(executorService);

        log.info("Binding HttpClient...");
        install(new HttpModule(executorService));

        log.info("Binding Halo API stuff...");
        bind(String.class).annotatedWith(Names.named("haloApiKey")).toInstance(configuration.haloApiKey);
        bind(MetadataCache.class).asEagerSingleton();

        log.info("Binding WebClient...");
        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.getCookieManager().setCookiesEnabled(false);
        bind(WebClient.class).toInstance(webClient);

        log.info("Binding twitter client...");
        bind(Twitter.class).toInstance(TwitterFactory.getSingleton());

        try {
            log.info("Binding job scheduler...");
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
        log.info("Bound DBConnectionString: {}", configuration.database);

        File filenameLibrary = new File(configuration.filenamesDir);
        if(!filenameLibrary.exists())
            filenameLibrary.mkdir();
        bind(File.class)
                .annotatedWith(Names.named("filenameLibrary"))
                .toInstance(filenameLibrary);
        log.info("Bound filename library directory to {}", filenameLibrary.getAbsolutePath());

        log.info("Binding JDBC connection provider...");
        bind(Connection.class).toProvider(DBConnectionProvider.class);
        bind(JdbcConnectionSource.class).toProvider(JdbcConnectionProvider.class);

        log.info("Binding random...");
        bind(Random.class).toInstance(new Random());

        // path to ffmpeg executable
        bind(String.class)
                .annotatedWith(Names.named("ffmpegExec"))
                .toInstance(configuration.ffmpegExec);
        log.info("Bound ffmpeg executable to {}", configuration.ffmpegExec);

        bind(String.class).annotatedWith(Names.named("ffprobeExec"))
                .toInstance(configuration.ffprobeExec);
        log.info("Bound ffprobe executable to {}", configuration.ffprobeExec);

        log.info("Binding google credentials...");
        bind(GoogleConfig.class).toInstance(configuration.google);
        GoogleCredential googleCredential = new GoogleCredential.Builder()
                .setTransport(new NetHttpTransport())
                .setJsonFactory(new JacksonFactory())
                .setClientSecrets(configuration.google.clientId, configuration.google.clientSecret).build();
        googleCredential.setRefreshToken(configuration.google.refreshToken);

        log.info("Binding youtube client...");
        YouTube youTube = new YouTube.Builder(
                googleCredential.getTransport(),
                googleCredential.getJsonFactory(),
                googleCredential)
                .setApplicationName(configuration.google.appId)
                .build();

        bind(YouTube.class).toInstance(youTube);

        install(new TSDTVModule(configuration.tsdtv));
        install(new TSDFMModule(configuration.tsdfm));

        log.info("TSDBotModule.configure() successful");

    }



}
