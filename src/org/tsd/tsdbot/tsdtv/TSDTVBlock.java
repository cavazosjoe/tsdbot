package org.tsd.tsdbot.tsdtv;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.functions.TSDTV;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by Joe on 3/16/14.
 */
public class TSDTVBlock implements Job {

    private static final Logger logger = LoggerFactory.getLogger(TSDTVBlock.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String name = jobExecutionContext.getJobDetail().getJobDataMap().getString(TSDTV.BLOCK_FIELD_NAME);
        String schedule = jobExecutionContext.getJobDetail().getJobDataMap().getString(TSDTV.BLOCK_FIELD_SCHEDULE);
        String id = jobExecutionContext.getJobDetail().getJobDataMap().getString(TSDTV.BLOCK_FIELD_ID);
        String[] scheduleParts = schedule.split(TSDTV.BLOCK_SCHEDULE_DELIMITER);

        LinkedList<String> blockSchedule = new LinkedList<>();
        Collections.addAll(blockSchedule, scheduleParts);

        try {
            TSDTV.getInstance().prepareScheduledBlock(id, name, blockSchedule, 0);
        } catch (SQLException e) {
            logger.error("Error preparing scheduled block", e);
        }

    }
}
