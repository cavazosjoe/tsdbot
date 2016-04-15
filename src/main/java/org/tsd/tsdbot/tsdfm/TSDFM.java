package org.tsd.tsdbot.tsdfm;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.config.TSDFMConfig;
import org.tsd.tsdbot.scheduled.SchedulerConstants;

import java.io.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Singleton
public class TSDFM {

    private static final Logger log = LoggerFactory.getLogger(TSDFM.class);

    private TSDFMQueueItem nowPlaying;
    private final Queue<TSDFMQueueItem> queue = new ConcurrentLinkedQueue<>();

    private final Bot bot;
    private final TSDFMLibrary library;
    private final Scheduler scheduler;
    private final String scheduleFile;

    @Inject
    public TSDFM(Bot bot,
                 TSDFMLibrary library,
                 Scheduler scheduler,
                 TSDFMConfig config,
                 ) {
        this.bot = bot;
        this.library = library;
        this.scheduler = scheduler;
        this.scheduleFile = config.scheduleFile;
    }

    public synchronized void playScheduledBlock(TSDFMBlock block) {
        queue.clear();

    }

    public void readSchedule() throws TSDFMScheduleException {
        try {
            log.info("Building TSDFM schedule...");
            GroupMatcher<JobKey> groupMatcher = GroupMatcher.<JobKey>groupEquals(SchedulerConstants.TSDTV_GROUP_ID);
            Set<JobKey> keys = scheduler.getJobKeys(groupMatcher);
            scheduler.pauseJobs(groupMatcher);
            scheduler.deleteJobs(new LinkedList<>(keys));

            FileInputStream schedule = new FileInputStream(new File(scheduleFile));
            try(BufferedReader br = new BufferedReader(new InputStreamReader(schedule))) {
                String line;

                String[] parts;
                String key;
                String value;

                String id = null;
                String name = null;
                String tags = null;
                String cronString = null;
                Integer duration = null;
                StringBuilder introBuilder = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    parts = line.split("=");
                    if (parts.length == 2) {
                        key = parts[0];
                        value = parts[1];
                        switch (key) {
                            case "id": {
                                // process existing stuff, if available
                                if (id != null) {
                                    scheduleWithInfo(id, name, tags, cronString, duration, introBuilder.toString());
                                }
                                id = value;
                                name = null;
                                tags = null;
                                cronString = null;
                                duration = null;
                                introBuilder = new StringBuilder();
                                break;
                            }
                            case "name": {
                                name = value;
                                break;
                            }
                            case "tags": {
                                tags = value;
                                break;
                            }
                            case "schedule": {
                                cronString = value;
                                break;
                            }
                            case "duration": {
                                duration = Integer.parseInt(value);
                                break;
                            }
                            case "intro": {
                                introBuilder.append(value);
                                break;
                            }
                        }

                    } else if (introBuilder.length() > 0) {
                        // this line isn't of the format key=value
                        // if we're currently capturing the intro, append this line
                        introBuilder.append(" ").append(line);
                    }
                }

                if (id != null) {
                    scheduleWithInfo(id, name, tags, cronString, duration, introBuilder.toString());
                }

            } catch (IOException ioe) {
                log.error("Error reading TSDFM schedule file");
                throw new TSDFMScheduleException("Error reading TSDFM schedule file");
            }

            scheduler.resumeAll();

        } catch (SchedulerException se) {
            log.error("Scheduler error while building TSDFM schedule", se);
            throw new TSDFMScheduleException(se.getMessage());
        } catch (FileNotFoundException fe) {
            log.error("Could not find TSDFM schedule file at {}", scheduleFile);
            throw new TSDFMScheduleException("Could not find TSDFM schedule file at %s", scheduleFile);
        }
    }

    void scheduleWithInfo(String id, String name, String tags,
                          String cronString, Integer duration, String intro) throws TSDFMScheduleException {

        // validate
        if(StringUtils.isBlank(name)
                || StringUtils.isBlank(tags)
                || StringUtils.isBlank(cronString)
                || duration == null
                || StringUtils.isBlank(intro)) {
            log.error("Error building schedule");
            throw new TSDFMScheduleException("Error building schedule: " +
                    "id=%s | name=%s | tags=%s | cronString=%s | duration=%s | intro=%s",
                    id,
                    name,
                    tags,
                    cronString,
                    duration == null ? "null" : duration.toString(),
                    intro);
        }

        JobDetail job = newJob(TSDFMBlockJob.class)
                .withIdentity(id, SchedulerConstants.TSDFM_BLOCK_ID)
                .usingJobData(SchedulerConstants.TSDFM_BLOCK_ID, id)
                .usingJobData(SchedulerConstants.TSDFM_BLOCK_NAME, name)
                .usingJobData(SchedulerConstants.TSDFM_BLOCK_TAGS, tags)
                .usingJobData(SchedulerConstants.TSDFM_BLOCK_SCHEDULE, cronString)
                .usingJobData(SchedulerConstants.TSDFM_BLOCK_DURATION, duration)
                .usingJobData(SchedulerConstants.TSDFM_BLOCK_INTRO, intro)
                .build();

        CronTrigger cronTrigger = newTrigger()
                .withSchedule(cronSchedule(cronString))
                .build();

        try {
            scheduler.scheduleJob(job, cronTrigger);
        } catch (SchedulerException se) {
            log.error("Scheduling error", se);
            throw new TSDFMScheduleException("Error building schedule: " + se.getMessage());
        }
    }

    File addIntroToSong(String intro text)

}
