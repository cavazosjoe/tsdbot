package org.tsd.tsdbot;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.scheduled.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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

        if(args.length < 5) {
            throw new Exception("USAGE: TSDBot.jar [channel] [server] [nick] [properties location] [stage=dev,production]");
        }

        String[] channels;
        String channel = args[0];
        if(!channel.startsWith("#")) channel = "#"+channel;
        channels = new String[]{channel};

        String server = args[1];
        String botName = args[2];
        String propertiesLocation = args[3];
        Stage stage = Stage.fromString(args[4]);
        if(stage == null) {
            throw new Exception("STAGE must be one of [dev, production]");
        }

        log.info("channel={}, server={} , name={} , propLoc={}, stage={}", channel, server, botName, propertiesLocation, stage);

        Properties properties = new Properties();
        try(InputStream fis = new FileInputStream(new File(propertiesLocation))) {
            properties.load(fis);
        }

        String nickservPass = properties.getProperty("nickserv.pass");
        TSDBot bot = new TSDBot(botName, nickservPass, server, channels);

        TSDBotConfigModule module = new TSDBotConfigModule(bot, properties, stage);

        Injector injector = Guice.createInjector(module);
        configureScheduler(injector);
        injector.injectMembers(TSDBot.class);

        log.info("TSDBot loaded successfully. Beginning conquest...");
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
}
