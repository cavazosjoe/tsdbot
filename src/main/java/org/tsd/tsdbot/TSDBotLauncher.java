package org.tsd.tsdbot;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.servlet.GuiceFilter;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.config.TSDBotConfiguration;
import org.tsd.tsdbot.module.ServerPort;
import org.tsd.tsdbot.module.TSDBotFunctionalModule;
import org.tsd.tsdbot.module.TSDBotModule;
import org.tsd.tsdbot.module.TSDBotServletModule;
import org.tsd.tsdbot.scheduled.*;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class TSDBotLauncher {

    private static Logger log = LoggerFactory.getLogger(TSDBotLauncher.class);

    // TSDBot.jar tsd-test irc.teamschoolyd.org TSDBot /path/to/tsdbot.properties dev
    public static void main(String[] args) throws Exception {

        if(args.length != 1) {
            throw new Exception("USAGE: TSDBot.jar [properties location]");
        }

        String configLocation = args[0];
        YamlReader yamlReader = new YamlReader(new FileReader(configLocation));
        TSDBotConfiguration config = yamlReader.read(TSDBotConfiguration.class);

        String ident = config.connection.ident;
        String nick = config.connection.nick;
        String pass = config.connection.nickservPass;
        String server = config.connection.server;

        Stage stage = config.connection.stage;
        if(stage == null) {
            throw new Exception("STAGE must be one of [dev, production]");
        }

        log.info("ident={}, nick={}, pass=***, server={}, stage={}",
                new Object[]{ident, nick, server, stage});

        TSDBot bot = new TSDBot(ident, nick, pass, server);

        Injector injector = null;
        try {
            TSDBotModule module = new TSDBotModule(bot, config);
            TSDBotServletModule servletModule = new TSDBotServletModule();
            TSDBotFunctionalModule functionalModule = new TSDBotFunctionalModule();
            log.info("Creating injector...");
            injector = Guice.createInjector(module, functionalModule, servletModule);
            log.info("Injector created successfully");
        } catch (Exception e) {
            throw new Exception("Error creating injector", e);
        }

        configureScheduler(injector);
        log.info("Scheduler configured successfully");
        injector.injectMembers(Bot.class);
        log.info("Bot injected successfully");

        log.info("TSDBot loaded successfully. Starting server...");
        initializeJettyServer(injector);

        bot.joinChannel(config.connection.mainChannel);
        log.info("Joined main channel {}", config.connection.mainChannel);
        for(String channel : config.connection.auxChannels) {
            bot.joinChannel(channel);
            log.info("Joined aux channel {}", channel);
        }

    }

    private static void initializeJettyServer(Injector injector) throws Exception {
        Server httpServer = new Server();
        ServerConnector connector = new ServerConnector(httpServer);
        int port = injector.getInstance(Key.get(Integer.class, ServerPort.class));
        connector.setPort(port);
        httpServer.addConnector(connector);

        URL indexUri = TSDBotLauncher.class.getResource("/webroot/");

        System.setProperty("org.apache.jasper.compiler.disablejsr199", "false");

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(), "embedded-jetty-jsp");
        if(!scratchDir.exists())
            scratchDir.mkdirs();

        JettyJasperInitializer sci = new JettyJasperInitializer();
        ContainerInitializer initializer = new ContainerInitializer(sci, null);
        List<ContainerInitializer> initializers = new ArrayList<>();
        initializers.add(initializer);

        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], TSDBotLauncher.class.getClassLoader());

        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.7");
        holderJsp.setInitParameter("compilerSourceVM", "1.7");
        holderJsp.setInitParameter("keepgenerated", "true");

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setAttribute("javax.servlet.context.tempdir", scratchDir);
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*jar$|.*/classes/.*");
        context.setResourceBase(indexUri.toURI().toASCIIString());
        context.setAttribute("org.eclipse.jetty.containerInitializers", initializers);
        context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        context.addBean(new ServletContainerInitializersStarter(context), true);
        context.setClassLoader(jspClassLoader);

        context.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

        context.addServlet(DefaultServlet.class, "/");

        httpServer.setHandler(context);

        httpServer.start();

        String scheme = "http";
        for (ConnectionFactory connectFactory : connector.getConnectionFactories()) {
            if (connectFactory.getProtocol().equals("SSL-http")) {
                scheme = "https";
            }
        }
        String host = connector.getHost();
        if (host == null) {
            host = "localhost";
        }
        port = connector.getLocalPort();
        URI serverURI = new URI(String.format("%s://%s:%d/", scheme, host, port));
        log.info("Server started, URI = {}", serverURI);
    }

    private static void configureScheduler(Injector injector) {
        try {
            TSDBotConfiguration config = injector.getInstance(TSDBotConfiguration.class);
            Scheduler scheduler = injector.getInstance(Scheduler.class);
            scheduler.setJobFactory(injector.getInstance(InjectableJobFactory.class));

            JobDetail logCleanerJob = newJob(LogCleanerJob.class)
                    .withIdentity(SchedulerConstants.LOG_JOB_KEY)
                    .usingJobData(SchedulerConstants.LOGS_DIR_FIELD, config.archivist.logs)
                    .build();

            JobDetail recapCleanerJob = newJob(RecapCleanerJob.class)
                    .withIdentity(SchedulerConstants.RECAP_JOB_KEY)
                    .usingJobData(SchedulerConstants.RECAP_DIR_FIELD, config.archivist.recaps)
                    .build();

            JobDetail printoutCleanerJob = newJob(PrintoutCleanerJob.class)
                    .withIdentity(SchedulerConstants.PRINTOUT_JOB_KEY)
                    .usingJobData(SchedulerConstants.PRINTOUT_DIR_FIELD, config.printoutDir)
                    .build();

//            JobDetail notificationJob = newJob(NotificationSweeperJob.class)
//                    .withIdentity(SchedulerConstants.NOTIFICATION_JOB_KEY)
//                    .build();

            CronTrigger logCleanerTrigger = newTrigger()
                    .withSchedule(cronSchedule("0 0 4 ? * MON")) //4AM every monday
                    .build();

            CronTrigger recapCleanerTrigger = newTrigger()
                    .withSchedule(cronSchedule("0 0 3 * * ?")) //3AM every day
                    .build();

            CronTrigger printoutCleanerTrigger = newTrigger()
                    .withSchedule(cronSchedule("0 0 3 * * ?"))
                    .build();

//            CronTrigger notifyTrigger = newTrigger()
//                    .withSchedule(cronSchedule("0 0/5 * * * ?")) //every 5 minutes
//                    .build();

            scheduler.scheduleJob(logCleanerJob, logCleanerTrigger);
            scheduler.scheduleJob(recapCleanerJob, recapCleanerTrigger);
            scheduler.scheduleJob(printoutCleanerJob, printoutCleanerTrigger);
//            scheduler.scheduleJob(notificationJob, notifyTrigger);

            scheduler.start();

        } catch (Exception e) {
            log.error("ERROR INITIALIZING SCHEDULED SERVICES", e);
        }
    }
}
