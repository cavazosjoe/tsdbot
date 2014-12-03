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
public class RecapCleanerJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(RecapCleanerJob.class);

    @Inject
    protected TSDBot bot;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String recapDirPath = jobExecutionContext.getJobDetail().getJobDataMap().getString(SchedulerConstants.RECAP_DIR_FIELD);

        Calendar recapCutoff = Calendar.getInstance();
        recapCutoff.add(Calendar.DATE, -1);
        File recapDir = new File(recapDirPath);
        try {
            if (recapDir.exists() && recapDir.isDirectory()) {
                for (File f : recapDir.listFiles()) {
                    if (recapCutoff.getTime().getTime() > f.lastModified()) {
                        Files.delete(f.toPath());
                    }
                }
            } else {
                logger.error("Could not find recap directory {}", recapDirPath);
                throw new FileNotFoundException("Could not find recap directory " + recapDirPath);
            }
        } catch(Exception e) {
            logger.error("An error occurred while cleaning recaps", e);
            bot.broadcast("An error occurred while cleaning recaps");
        }
    }
}
