package org.tsd.tsdbot.scheduled;

import org.quartz.JobKey;

public class SchedulerConstants {

    public static final String TSDTV_GROUP_ID = "tsdtv";
    public static final String TSDFM_GROUP_ID = "tsdfm";

    public static final JobKey LOG_JOB_KEY = new JobKey("log-job");

    // TSDTVBlockJob
    public static final String TSDTV_BLOCK_ID_FIELD = "id";
    public static final String TSDTV_BLOCK_NAME_FIELD = "name";
    public static final String TSDTV_BLOCK_SCHEDULE_FIELD = "schedule";
    public static final String TSDTV_BLOCK_SCHEDULE_DELIMITER = ";;";

    // TSDFMBlockJob
    public static final String TSDFM_BLOCK_ID = "id";
    public static final String TSDFM_BLOCK_NAME = "name";
    public static final String TSDFM_BLOCK_TAGS = "tags";
    public static final String TSDFM_BLOCK_SCHEDULE = "schedule";
    public static final String TSDFM_BLOCK_DURATION = "duration";
    public static final String TSDFM_BLOCK_INTRO = "intro";

    // RecapCleanerJob
    public static final String LOGS_DIR_FIELD = "logsDir";

}
