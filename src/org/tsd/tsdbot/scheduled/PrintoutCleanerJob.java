package org.tsd.tsdbot.scheduled;

import com.google.inject.Inject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.Calendar;

/**
 * Created by Joe on 8/24/2014.
 */
public class PrintoutCleanerJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(PrintoutCleanerJob.class);

    @Inject
    protected TSDBot bot;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String printoutDirPath = jobExecutionContext.getJobDetail().getJobDataMap().getString(SchedulerConstants.PRINTOUT_DIR_FIELD);

        Calendar printoutCutoff = Calendar.getInstance();
        printoutCutoff.add(Calendar.DATE, -1);
        File printoutDir = new File(printoutDirPath);
        try {
            if (printoutDir.exists() && printoutDir.isDirectory()) {
                for (File f : printoutDir.listFiles()) {
                    if (printoutCutoff.getTime().getTime() > f.lastModified()) {
                        Files.delete(f.toPath());
                    }
                }
            } else {
                logger.error("Could not find printout directory {}", printoutDirPath);
                throw new FileNotFoundException("Could not find printout directory " + printoutDirPath);
            }
        } catch(Exception e) {
            logger.error("An error occurred while cleaning printouts", e);
            bot.broadcast("An error occurred while cleaning printouts");
        }
    }
}
