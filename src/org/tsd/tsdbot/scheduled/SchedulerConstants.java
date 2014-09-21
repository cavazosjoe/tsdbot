package org.tsd.tsdbot.scheduled;

import org.quartz.JobKey;

/**
 * Created by Joe on 8/24/2014.
 */
public class SchedulerConstants {

    public static final String TSDTV_GROUP_ID = "tsdtv";

    public static final JobKey LOG_JOB_KEY = new JobKey("log-job");
    public static final JobKey RECAP_JOB_KEY = new JobKey("recap-job");
    public static final JobKey NOTIFICATION_JOB_KEY = new JobKey("notify-job");

    //TSDTVBlockJob
    public static final String TSDTV_BLOCK_ID_FIELD = "id";
    public static final String TSDTV_BLOCK_NAME_FIELD = "name";
    public static final String TSDTV_BLOCK_SCHEDULE_FIELD = "schedule";
    public static final String TSDTV_BLOCK_SCHEDULE_DELIMITER = ";;";

    //RecapCleanerJob
    public static final String LOGS_DIR_FIELD = "logsDir";

    //ArchiveCleanerJob
    public static final String RECAP_DIR_FIELD = "recapDir";
}
