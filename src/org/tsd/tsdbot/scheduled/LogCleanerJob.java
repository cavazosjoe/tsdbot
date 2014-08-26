package org.tsd.tsdbot.scheduled;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;

import java.io.*;
import java.util.Calendar;

/**
 * Created by Joe on 8/24/2014.
 */
public class LogCleanerJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(LogCleanerJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String logsDirPath = jobExecutionContext.getJobDetail().getJobDataMap().getString(SchedulerConstants.LOGS_DIR_FIELD);

        //TODO: add compression/archiving of old logs
        Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.DATE, -2); // erase everything older than 2 days
        long cutoffMillis = cutoff.getTimeInMillis();
        File logsDir = new File(logsDirPath);
        try {
            if (logsDir.exists() && logsDir.isDirectory()) {
                for (File f : logsDir.listFiles()) {
                    if(f.isFile() && f.getName().endsWith(".log")) {
                        File tempFile = File.createTempFile("logg",".log");
                        try(BufferedReader reader = new BufferedReader(new FileReader(f));
                            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                            String line;
                            Long messageTime;
                            while ((line = reader.readLine()) != null) {
                                messageTime = Long.valueOf(line.split("\\s")[1]);
                                if (messageTime > cutoffMillis) {
                                    writer.write(line);
                                    writer.newLine();
                                }
                            }
                            if(!tempFile.renameTo(f))
                                throw new IOException("Could not rename temp file to existing log");
                        } catch (Exception e) {
                            logger.error("An error occurred while trimming {}", f.getAbsolutePath());
                            throw e;
                        } finally {
                            tempFile.delete();
                        }
                    }
                }
            } else {
                logger.error("Could not find logs directory {}", logsDirPath);
                throw new FileNotFoundException("Could not find logs directory " + logsDirPath);
            }
        } catch(Exception e) {
            logger.error("An error occurred while cleaning logs", e);
            TSDBot.getInstance().broadcast("An error occurred while cleaning logs");
        }
    }
}
