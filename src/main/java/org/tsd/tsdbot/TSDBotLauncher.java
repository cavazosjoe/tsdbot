package org.tsd.tsdbot;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.servlet.GuiceFilter;
import com.sun.faces.config.ConfigureListener;
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
import org.tsd.tsdbot.scheduled.*;

import javax.faces.webapp.FacesServlet;
import javax.servlet.DispatcherType;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBotLauncher {

    private static Logger log = LoggerFactory.getLogger(TSDBotLauncher.class);

    // TSDBot.jar tsd-test irc.teamschoolyd.org TSDBot /path/to/tsdbot.properties dev
    public static void main(String[] args) throws Exception {

        if(args.length != 1) {
            throw new Exception("USAGE: TSDBot.jar [properties location]");
        }

        String propertiesLocation = args[0];
        Properties properties = new Properties();
        try(InputStream fis = new FileInputStream(new File(propertiesLocation))) {
            properties.load(fis);
        }

        String ident = properties.getProperty("ident");
        String nick = properties.getProperty("nick");
        String pass = properties.getProperty("nickserv.pass");
        String server = properties.getProperty("server");
        String[] channels = properties.getProperty("channels").split(",");
        Stage stage = Stage.fromString(properties.getProperty("stage"));
        if(stage == null) {
            throw new Exception("STAGE must be one of [dev, production]");
        }

        log.info("ident={}, nick={}, pass=***, server={}, channels={}, stage={}",
                new Object[]{ident, nick, server, channels, stage});

        TSDBot bot = new TSDBot(ident, nick, pass, server, channels);

        TSDBotConfigModule module = new TSDBotConfigModule(bot, properties, stage);
        TSDBotServletModule servletModule = new TSDBotServletModule();

        Injector injector = Guice.createInjector(module, servletModule);
        configureScheduler(injector);
        injector.injectMembers(TSDBot.class);

        log.info("TSDBot loaded successfully. Starting server...");
        initializeJettyServer(injector);
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
            Properties properties = injector.getInstance(Properties.class);
            Scheduler scheduler = injector.getInstance(Scheduler.class);
            scheduler.setJobFactory(injector.getInstance(InjectableJobFactory.class));

            JobDetail logCleanerJob = newJob(LogCleanerJob.class)
                    .withIdentity(SchedulerConstants.LOG_JOB_KEY)
                    .usingJobData(SchedulerConstants.LOGS_DIR_FIELD, properties.getProperty("archivist.logs"))
                    .build();

            JobDetail recapCleanerJob = newJob(RecapCleanerJob.class)
                    .withIdentity(SchedulerConstants.RECAP_JOB_KEY)
                    .usingJobData(SchedulerConstants.RECAP_DIR_FIELD, properties.getProperty("archivist.recaps"))
                    .build();

            JobDetail printoutCleanerJob = newJob(PrintoutCleanerJob.class)
                    .withIdentity(SchedulerConstants.PRINTOUT_JOB_KEY)
                    .usingJobData(SchedulerConstants.PRINTOUT_DIR_FIELD, properties.getProperty("printout.dir"))
                    .build();

            JobDetail notificationJob = newJob(NotificationSweeperJob.class)
                    .withIdentity(SchedulerConstants.NOTIFICATION_JOB_KEY)
                    .build();

            CronTrigger logCleanerTrigger = newTrigger()
                    .withSchedule(cronSchedule("0 0 4 ? * MON")) //4AM every monday
                    .build();

            CronTrigger recapCleanerTrigger = newTrigger()
                    .withSchedule(cronSchedule("0 0 3 * * ?")) //3AM every day
                    .build();

            CronTrigger printoutCleanerTrigger = newTrigger()
                    .withSchedule(cronSchedule("0 0 3 * * ?"))
                    .build();

            CronTrigger notifyTrigger = newTrigger()
                    .withSchedule(cronSchedule("0 0/5 * * * ?")) //every 5 minutes
                    .build();

            scheduler.scheduleJob(logCleanerJob, logCleanerTrigger);
            scheduler.scheduleJob(recapCleanerJob, recapCleanerTrigger);
            scheduler.scheduleJob(printoutCleanerJob, printoutCleanerTrigger);
            scheduler.scheduleJob(notificationJob, notifyTrigger);

            scheduler.start();

        } catch (Exception e) {
            log.error("ERROR INITIALIZING SCHEDULED SERVICES", e);
        }
    }

//    private static void writeCommandList(Properties properties) {
//        String cmdListLoc = properties.getProperty("commandList");
//        log.info("Writing command list to {}...", cmdListLoc);
//        try(BufferedWriter commandListWriter = new BufferedWriter(new FileWriter(cmdListLoc))) {
//            boolean first = true;
//            for(Command cmd : Command.values()) {
//                if(cmd.getDesc() != null) {
//                    if(!first) commandListWriter.write("-----------------------------------------\n");
//                    commandListWriter.write(cmd.getDesc() + "\n");
//                    commandListWriter.write(cmd.getUsage() + "\n");
//                    first = false;
//                }
//            }
//        } catch (Exception e) {
//            log.error("ERROR PRINTING COMMAND LIST", e);
//        }
//    }
}
