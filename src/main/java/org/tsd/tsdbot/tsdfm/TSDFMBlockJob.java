package org.tsd.tsdbot.tsdfm;

import com.google.inject.Inject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.scheduled.SchedulerConstants;

public class TSDFMBlockJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(TSDFMBlockJob.class);

    @Inject
    protected TSDFM tsdfm;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

        String[] tags = jobDataMap.getString(SchedulerConstants.TSDFM_BLOCK_TAGS).split(",");

        TSDFMBlock block = new TSDFMBlock(
                jobDataMap.getString(SchedulerConstants.TSDFM_BLOCK_ID),
                jobDataMap.getString(SchedulerConstants.TSDFM_BLOCK_NAME),
                jobDataMap.getString(SchedulerConstants.TSDFM_BLOCK_INTRO),
                jobDataMap.getInt(SchedulerConstants.TSDFM_BLOCK_DURATION),
                tags
        );

        tsdfm.playScheduledBlock(block);
    }
}
