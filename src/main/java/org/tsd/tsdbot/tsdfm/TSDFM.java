package org.tsd.tsdbot.tsdfm;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.config.TSDFMConfig;
import org.tsd.tsdbot.scheduled.SchedulerConstants;
import org.tsd.tsdbot.tsdfm.model.TSDFMSong;
import org.tsd.tsdbot.util.FfmpegUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Singleton
public class TSDFM {

    private static final Logger log = LoggerFactory.getLogger(TSDFM.class);

    // minimum number of songs to keep in the queue
    private static final int SONGS_IN_QUEUE = 3;

    private TSDFMQueueItem nowPlaying;
    private final Queue<TSDFMQueueItem> queue = new ConcurrentLinkedQueue<>();

    private final Bot bot;
    private final TSDFMLibrary library;
    private final Scheduler scheduler;
    private final String scheduleFile;
    private final TSDFMFileProcessor fileProcessor;
    private final FfmpegUtils ffmpegUtils;

    @Inject
    public TSDFM(Bot bot,
                 TSDFMLibrary library,
                 TSDFMFileProcessor fileProcessor,
                 Scheduler scheduler,
                 FfmpegUtils ffmpegUtils,
                 TSDFMConfig config) {
        this.bot = bot;
        this.library = library;
        this.scheduler = scheduler;
        this.scheduleFile = config.scheduleFile;
        this.fileProcessor = fileProcessor;
        this.ffmpegUtils = ffmpegUtils;
    }

    public synchronized void playScheduledBlock(TSDFMBlock block) {
        try {

            LinkedList<TSDFMSong> songsWithTags = new LinkedList<>(library.getAllSongsForTags(block.getTagsToPlay()));
            Collections.shuffle(songsWithTags);

            List<TSDFMQueueItem> filesToPlay = new LinkedList<>();
            long blockTimeMillis = TimeUnit.MINUTES.toMillis(block.getDuration());
            long accumulatedBlockMillis = 0;
            TSDFMSong song;
            File processedSong;
            while(accumulatedBlockMillis < blockTimeMillis && songsWithTags.size() > 0) {
                song = songsWithTags.pop();
                if(accumulatedBlockMillis == 0) {
                    processedSong = fileProcessor.addIntroToSong(block.getIntro(), song);
                } else {
                    processedSong = fileProcessor.addIntroToSong(
                        SongTransitions.get(song.getArtist().getName(), song.getTitle()),
                        song
                    );
                }

                long songDuration = ffmpegUtils.getDuration(processedSong);
                filesToPlay.add(new TSDFMQueueItem(processedSong, song, block));
                accumulatedBlockMillis += songDuration;
            }

            queue.addAll(filesToPlay);

        } catch (Exception e) {
            log.error("Error playing scheduled block", e);
            bot.sendMessage();
        }

    }

    synchronized void play(TSDFMQueueItem queueItem) {

    }

    public synchronized void playNext() {

        if(nowPlaying != null) {
            nowPlaying.getFile().delete();
            nowPlaying = null;
        }

        while(queue.size() <= SONGS_IN_QUEUE) try {
            TSDFMSong song = library.getRandomSong();
            File processedSong = fileProcessor.addIntroToSong(
                SongTransitions.get(song.getArtist().getName(), song.getTitle()),
                song
            );
            TSDFMQueueItem queueItem = new TSDFMQueueItem(processedSong, song);
            queue.add(queueItem);
        } catch (Exception e) {
            log.error("Error loading item into queue", e);
        }

        play(queue.poll());
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

}
