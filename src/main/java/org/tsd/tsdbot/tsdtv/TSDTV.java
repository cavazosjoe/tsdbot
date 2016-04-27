package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.jibble.pircbot.User;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.config.TSDBotConfiguration;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.Persistable;
import org.tsd.tsdbot.module.TSDTVChannels;
import org.tsd.tsdbot.scheduled.SchedulerConstants;
import org.tsd.tsdbot.tsdtv.model.FillerType;
import org.tsd.tsdbot.tsdtv.model.TSDTVEpisode;
import org.tsd.tsdbot.tsdtv.model.TSDTVFiller;
import org.tsd.tsdbot.tsdtv.model.TSDTVShow;
import org.tsd.tsdbot.tsdtv.processor.FileAnalysis;
import org.tsd.tsdbot.tsdtv.processor.StreamType;
import org.tsd.tsdbot.util.FfmpegUtils;
import org.tsd.tsdbot.util.fuzzy.FuzzyLogic;
import org.tsd.tsdbot.util.fuzzy.FuzzyVisitor;

import javax.naming.AuthenticationException;
import java.io.*;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.tsd.tsdbot.util.IRCUtil.IRCColor;
import static org.tsd.tsdbot.util.IRCUtil.color;

@Singleton
public class TSDTV implements Persistable {

    private static Logger log = LoggerFactory.getLogger(TSDTV.class);

    private static final String showsTable = "TSDTV_SHOW";
    private static final int dayBoundaryHour = 4; // 4:00 AM
    private static final TimeZone timeZone = TimeZone.getTimeZone("America/New_York");

    private final Bot bot;

    private final TSDTVLibrary library;
    private final TSDTVFileProcessor processor;

    private final DBConnectionProvider connectionProvider;
    private final InjectableStreamFactory streamFactory;
    private final Scheduler scheduler;
    private final String scheduleLoc;
    private final String serverUrl;
    private final String tsdtvDirect;
    private final FfmpegUtils ffmpegUtils;
    private final List<String> tsdtvChannels;

    private LockdownMode lockdownMode = LockdownMode.open;

    private LinkedList<TSDTVQueueItem> queue = new LinkedList<>(); // file paths
    private TSDTVStream runningStream;

    @Inject
    public TSDTV(Bot bot,
                 TSDTVLibrary library,
                 TSDTVFileProcessor fileProcessor,
                 TSDBotConfiguration config,
                 Scheduler scheduler,
                 DBConnectionProvider connectionProvider,
                 InjectableStreamFactory streamFactory,
                 FfmpegUtils ffmpegUtils,
                 @Named("serverUrl") String serverUrl,
                 @Named("tsdtvDirect") String tsdtvDirect,
                 @TSDTVChannels List tsdtvChannels) throws SQLException {
        log.info("Constructing TSDTV... numShows = {}", library.getAllShows().size());
        this.bot = bot;
        this.processor = fileProcessor;
        this.library = library;
        this.scheduleLoc = config.tsdtv.scheduleFile;
        this.scheduler = scheduler;
        this.connectionProvider = connectionProvider;
        this.streamFactory = streamFactory;
        this.serverUrl = serverUrl;
        this.tsdtvDirect = tsdtvDirect;
        this.ffmpegUtils = ffmpegUtils;
        this.tsdtvChannels = tsdtvChannels;
        initDB();
        buildSchedule();
    }

    @Override
    public void initDB() throws SQLException {
        log.info("Initializing TSDTV database");

        Connection connection = connectionProvider.get();

        // load new shows
        String createShows = String.format("create table if not exists %s (" +
                "id int auto_increment," +
                "name varchar," +
                "currentEpisode int," +
                "primary key (id))", showsTable);
        try(PreparedStatement ps = connection.prepareStatement(createShows)) {
            log.info("{}: {}", showsTable, createShows);
            ps.executeUpdate();
        }

        log.info("Building {} table...", showsTable);
        for(TSDTVShow show : library.getAllShows()) {
            String q = String.format("select count(*) from %s where name = ?", showsTable);
            try(PreparedStatement ps = connection.prepareStatement(q)) {
                ps.setString(1, show.getRawName());
                try(ResultSet result = ps.executeQuery()) {
                    result.next();
                    if (result.getInt(1) == 0) { // show does not exist in db, add it
                        log.info("Could not find show {} in DB, adding...", show.getRawName());
                        String insertShow = String.format(
                                "insert into %s (name, currentEpisode) values (?,1)",
                                showsTable);
                        try (PreparedStatement ps1 = connection.prepareCall(insertShow)) {
                            ps1.setString(1, show.getRawName());
                            ps1.executeUpdate();
                        }
                    } else {
                        log.info("Show {} already exists in DB, skipping...", show.getRawName());
                    }
                }
            }
        }
    }

    public void updateCurrentEpisode(TSDTVShow show, TSDTVEpisode episode) throws SQLException {

        // set the episode number in the database
        setEpisodeNumber(show, episode.getEpisodeNumber());

        // try some hot-swapping in the queue
        if(!queue.isEmpty()) {

            log.info("Hotswapping episodes in queue...");

            int updatingToEpisodeNum = episode.getEpisodeNumber();
            long timeOffset = 0; // running number of milliseconds to push downstream start/end times

            TSDTVQueueItem queueItem;
            ListIterator<TSDTVQueueItem> queueIterator = queue.listIterator();

            while(queueIterator.hasNext()) {

                queueItem = queueIterator.next();
                log.info("Evaluating file {}...", queueItem.video.getFile());

                if(queueItem.scheduled && queueItem.video.getEpisodeNumber() > 0) {
                    // this is a scheduled episode. check if it's our show, update if so
                    try {

                        TSDTVShow queueShow = ((TSDTVEpisode) queueItem.video).getShow();

                        if(show.equals(queueShow)) { // this is an episode of the show we're updating

                            log.info("Queue item matches show {}, updating...", show.getRawName());

                            TSDTVEpisode replacingWithEpisode = show.getEpisode(updatingToEpisodeNum);
                            log.info("Replacing with file {}...", replacingWithEpisode.getRawName());

                            TSDTVBlock block = queueItem.block;

                            Date startTime = new Date(queueItem.startTime.getTime() + timeOffset);
                            log.info("{} + {}ms = {}", new Object[] {queueItem.startTime, timeOffset, startTime});

                            long duration = ffmpegUtils.getDuration(replacingWithEpisode.getFile());
                            TSDTVQueueItem replacementQueueItem = new TSDTVQueueItem(
                                    replacingWithEpisode,
                                    block,
                                    true,
                                    startTime,
                                    duration,
                                    null
                            );

                            queueIterator.set(replacementQueueItem);
                            log.info("Replaced item in queue");

                            String broadcastFmt = "[TSDTV] Replaced episode in queue: %s, %s -> %s";
                            broadcast(String.format(
                                    broadcastFmt,
                                    show.getPrettyName(),
                                    ((TSDTVEpisode) queueItem.video).getPrettyName(),
                                    replacingWithEpisode.getPrettyName()));

                            // calculate time difference between old item and new, add to the offset
                            long timeDiff = ffmpegUtils.getDuration(replacingWithEpisode.getFile()) - ffmpegUtils.getDuration(queueItem.video.getFile());
                            timeOffset += timeDiff;
                            log.info("timeOffset + {} = {}", timeDiff + timeOffset);

                            // loop to episode 1 if we're at the end
                            if(updatingToEpisodeNum >= show.getAllEpisodes().size()) {
                                updatingToEpisodeNum = 1;
                            } else {
                                updatingToEpisodeNum++;
                            }

                            log.info("updatingToEpisodeNumber = {}", updatingToEpisodeNum);

                        } else {
                            // we're not replacing this episode, but its start time should be adjusted
                            log.info("Adjusting non-matched video's start time by " + timeOffset);
                            queueItem.startTime = new Date(queueItem.startTime.getTime() + timeOffset);
                        }

                    } catch (EpisodeNotFoundException e) {
                        log.error("Error updating downstream queue", e);
                        broadcast("Error updating downstream queue, please check logs");
                        return;
                    }

                } else {
                    // we're not replacing this episode, but its start time should be adjusted
                    log.info("Adjusting non-matched video's start time by " + timeOffset);
                    queueItem.startTime = new Date(queueItem.startTime.getTime() + timeOffset);
                }
            }
        }
    }

    public TreeMap<Date, TSDTVBlock> getRemainingBlocks(boolean todayOnly) throws SchedulerException {
        GregorianCalendar endOfToday = new GregorianCalendar();
        endOfToday.setTimeZone(timeZone);
        endOfToday.set(Calendar.HOUR_OF_DAY, dayBoundaryHour);
        if(endOfToday.get(Calendar.HOUR_OF_DAY) >= dayBoundaryHour)
            endOfToday.add(Calendar.DATE, 1);

        Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.<JobKey>groupEquals(SchedulerConstants.TSDTV_GROUP_ID));
        TreeMap<Date, TSDTVBlock> jobMap = new TreeMap<>();
        for(JobKey key : keys) {
            List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(key);
            if(!triggers.isEmpty()) {
                Date nextFireTime = triggers.get(0).getNextFireTime();
                if((!todayOnly) || nextFireTime.before(endOfToday.getTime()))
                    jobMap.put(nextFireTime, new TSDTVBlock(scheduler.getJobDetail(key).getJobDataMap()));
            }
        }

        return jobMap;
    }

    public void setLockdownMode(LockdownMode lockdownMode) {
        this.lockdownMode = lockdownMode;
    }

    public TSDTVStream getNowPlaying() {
        return runningStream;
    }

    public LinkedList<TSDTVQueueItem> getQueue() {
        return queue;
    }

    public ShowInfo getPrevAndNextEpisodeNums(String showQuery) throws ShowNotFoundException, SQLException {
        TSDTVShow show = library.getShow(showQuery);
        int nextEpisode = getCurrentEpisode(show);
        if(nextEpisode > show.getAllEpisodes().size())
            nextEpisode = 1;

        int prevEpisode = getCurrentEpisode(show) - 1;
        if(prevEpisode < 1)
            prevEpisode = show.getAllEpisodes().size();

        return new ShowInfo(show.getRawName(), prevEpisode, nextEpisode);
    }

    public boolean isRunning() {
        return runningStream != null;
    }

    private void play(TSDTVQueueItem program) {

        // determine if we have subtitles for this
        StringBuilder videoFilter = new StringBuilder();
        videoFilter.append("yadif");
        if(hasSubtitles(program.video.getFile())) {
            videoFilter.append(",subtitles=").append(program.video.getFile().getAbsolutePath());
        }

        runningStream = streamFactory.newStream(videoFilter.toString(), program);
        runningStream.start();

        if(program.scheduled && program.video.getEpisodeNumber() > 0) {
            try {
                TSDTVShow show = ((TSDTVEpisode)program.video).getShow();
                // loop to episode 1 if we're playing the last episode of the show
                int newEpNumber = (show.getAllEpisodes().size() <= program.video.getEpisodeNumber()) ? 1 : program.video.getEpisodeNumber()+1;
                setEpisodeNumber(show, newEpNumber);
            } catch (Exception e) {
                broadcast("Error updating show episode number. Please check logs");
                log.error("Error updating show episode number", e);
            }
        }

        if(program.video.isBroadcastable()) {
            HashMap<String,String> metadata = ffmpegUtils.getMetadata(program.video.getFile());
            String artist = metadata.get(TSDTVConstants.METADATA_ARTIST_FIELD);
            String title = metadata.get(TSDTVConstants.METADATA_TITLE_FIELD);

            StringBuilder sb = new StringBuilder();
            sb.append(color("[TSDTV]", IRCColor.blue)).append(" [NOW PLAYING]: ");
            if(artist == null || title == null)
                sb.append(program.video.toString());
            else
                sb.append(artist).append(": ").append(title);
            sb.append(" -- ").append(getLinks(false));

            broadcast(sb.toString());
        }
    }

    /**
     * method to play a movie from the chat. returns true if playing immediately, false if queued
     */
    public boolean playFromChat(TSDTVEpisode episode, User user) throws StreamLockedException {
        if(user.hasPriv(User.Priv.OP) || !lockdownMode.equals(LockdownMode.locked)) {
            TSDTVUser tsdtvUser = new TSDTVChatUser(user);
            long duration = ffmpegUtils.getDuration(episode.getFile());
            TSDTVQueueItem program = new TSDTVQueueItem(episode, null, false, getStartDateForQueueItem(), duration, tsdtvUser);
            if (isRunning()) {
                queue.addLast(program);
                return false;
            } else {
                play(program);
                return true;
            }
        } else {
            throw new StreamLockedException("The stream is currently locked down");
        }
    }

    /**
     * method to play a movie from the webpage catalog
     */
    public void playFromWeb(TSDTVEpisode episode, InetAddress inetAddress) throws StreamLockedException {
        if(lockdownMode.equals(LockdownMode.open)) {
            TSDTVUser tsdtvUser = new TSDTVWebUser(inetAddress);
            long duration = ffmpegUtils.getDuration(episode.getFile());
            TSDTVQueueItem program = new TSDTVQueueItem(episode, null, false, getStartDateForQueueItem(), duration, tsdtvUser);
            if (isRunning()) {
                queue.addLast(program);
                broadcast(color("[TSDTV]", IRCColor.blue)
                        + " A show has been enqueued via web: " + episode.getShow().getPrettyName() + " - " + episode.getPrettyName());
            } else {
                play(program);
            }
        } else {
            throw new StreamLockedException("The stream is locked from web access");
        }
    }

    public boolean authorized(TSDTVUser tsdtvUser) throws NoStreamRunningException {
        if(runningStream != null) {
            return tsdtvUser.isOp() || tsdtvUser.equals(runningStream.getMovie().owner);
        } else {
            throw new NoStreamRunningException();
        }
    }

    public void kill(TSDTVUser user) throws NoStreamRunningException, AuthenticationException {
        if(runningStream != null) {
            if(authorized(user))
                runningStream.kill(true);
            else
                throw new AuthenticationException();
        } else {
            throw new NoStreamRunningException();
        }
    }

    public void killAll(TSDTVUser user) throws AuthenticationException, NoStreamRunningException {
        if(runningStream != null) {
            if(user.isOp())
                runningStream.kill(false);
            else
                throw new AuthenticationException();
        } else {
            throw new NoStreamRunningException();
        }
    }

    public void pause(TSDTVUser user) throws NoStreamRunningException, IllegalStateException, AuthenticationException {
        if(runningStream != null) {
            if(authorized(user))
                runningStream.pauseStream();
            else
                throw new AuthenticationException();
        } else {
            throw new NoStreamRunningException();
        }
    }

    public void unpause(TSDTVUser user) throws NoStreamRunningException, IllegalStateException, AuthenticationException {
        if(runningStream != null) {
            if(authorized(user))
                runningStream.resumeStream();
            else
                throw new AuthenticationException();
        } else {
            throw new NoStreamRunningException();
        }
    }

    public void prepareScheduledBlock(TSDTVBlock blockInfo, int offset) throws SQLException {

        log.info("Preparing TSDTV block: {} with offset {}", blockInfo.name, offset);

        if(runningStream != null) {
            runningStream.interrupt(); // end running stream
            log.info("Ended currently running stream");
        }

        queue.clear();

        boolean scheduled = (offset == 0);

        // prepare the block intro if it exists
        TSDTVFiller blockIntro = library.getFiller(FillerType.block_intro, blockInfo.id);
        if(blockIntro != null) {
            long duration = ffmpegUtils.getDuration(blockIntro.getFile());
            queue.addLast(new TSDTVQueueItem(blockIntro, blockInfo, scheduled, getStartDateForQueueItem(), duration, null));
        }

        // use dynamic map to get correct episode numbers for repeating shows
        // use offset to handle replays/reruns
        HashMap<TSDTVShow, Integer> episodeNums = new HashMap<>(); // show -> episode num
        for(String showName : blockInfo.scheduleParts) {

            FillerType fillerType = FillerType.fromSchedule(showName);
            if(fillerType != null) {
                // this is filler, e.g. bump or commercial
                TSDTVFiller filler = library.getFiller(fillerType, null);
                if(filler != null) {
                    long duration = ffmpegUtils.getDuration(filler.getFile());
                    queue.addLast(new TSDTVQueueItem(filler, blockInfo, scheduled, getStartDateForQueueItem(), duration, null));
                    log.info("Added {} to queue", filler.getFile().getAbsolutePath());
                } else {
                    log.error("Could not find any filler of type {}", fillerType);
                }
            } else {

                // this isn't filler, it must be a TSDTVShow
                TSDTVShow show;
                try {
                    show = library.getShow(showName);
                } catch (ShowNotFoundException e) {
                    log.error("Could not find show", e);
                    continue;
                }

                // add the intro for this show, if it exists
                TSDTVFiller intro = library.getFiller(FillerType.show_intro, showName);
                if(intro != null) {
                    long duration = ffmpegUtils.getDuration(intro.getFile());
                    queue.addLast(new TSDTVQueueItem(intro, blockInfo, scheduled, getStartDateForQueueItem(), duration, null));
                }

                int occurrences = Collections.frequency(Arrays.asList(blockInfo.scheduleParts), showName);
                int episodeNum;
                if(!episodeNums.containsKey(show)) {
                    // this show hasn't appeared in the block yet -- get current episode num from DB
                    episodeNum = getCurrentEpisode(show) + (occurrences * offset);
                    if(episodeNum > 0)
                        episodeNums.put(show, episodeNum);
                    else
                        log.error("Could not find current episode for {}", show);
                } else {
                    // this show has appeared in the block -- increment episode num
                    if(episodeNums.get(show)+1 > show.getAllEpisodes().size())
                        episodeNum = 1; // wrap if we reached the end
                    else
                        episodeNum = episodeNums.get(show)+1;
                    episodeNums.put(show, episodeNum);
                }

                log.info("Looking for episode {} of {}", episodeNum, show.getRawName());
                TSDTVEpisode episode;
                try {
                    episode = show.getEpisode(episodeNum);
                } catch (EpisodeNotFoundException e) {
                    log.error("Could not find episode", e);
                    continue;
                }

                long duration = ffmpegUtils.getDuration(episode.getFile());
                queue.addLast(new TSDTVQueueItem(episode, blockInfo, scheduled, getStartDateForQueueItem(), duration, null));

                log.info("Added {} to queue", episode.getRawName());

            }
        }

        // prepare the block outro if it exists
        TSDTVFiller blockOutro = library.getFiller(FillerType.block_outro, blockInfo.id);
        if(blockOutro != null) {
            long duration = ffmpegUtils.getDuration(blockOutro.getFile());
            queue.addLast(new TSDTVQueueItem(blockOutro, blockInfo, scheduled, getStartDateForQueueItem(), duration, null));
        }

        StringBuilder broadcastBuilder = new StringBuilder();
        broadcastBuilder.append(color("[TSDTV]", IRCColor.blue)).append(" \"")
                .append(blockInfo.name).append("\" block now starting. Lined up: ");

        String lastProgram = "";
        boolean first = true;
        for(String program : blockInfo.scheduleParts) {
            if(program.startsWith(".")) continue;
            if(!lastProgram.equals(program)) {
                if(!first) broadcastBuilder.append(", ");
                broadcastBuilder.append(program);
                lastProgram = program;
                first = false;
            }
        }

        bot.broadcast(broadcastBuilder.toString());
        bot.broadcast(getLinks(false));

        if(!queue.isEmpty()) {
            play(queue.pop());
        } else {
            log.error("Could not find any shows for block...");
        }
    }

    public void prepareBlockReplay(String channel, String blockQuery) {

        log.info("Preparing TSDTV block rerun: {}", blockQuery);

        if(runningStream != null) {
            bot.sendMessage(channel, "There is already a stream running, please wait for it to" +
                    " finish  before starting a block rerun");
            return;
        }

        try {
            Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.<JobKey>groupEquals(SchedulerConstants.TSDTV_GROUP_ID));
            LinkedList<JobKey> matchedJobs = FuzzyLogic.fuzzySubset(blockQuery, new LinkedList<>(keys), new FuzzyVisitor<JobKey>() {
                @Override
                public String visit(JobKey o1) {
                    try {
                        JobDetail job = scheduler.getJobDetail(o1);
                        return job.getJobDataMap().getString(SchedulerConstants.TSDTV_BLOCK_NAME_FIELD);
                    } catch (SchedulerException e) {
                        log.error("Error getting job for key {}", o1.getName());
                    }
                    return null;
                }
            });

            if(matchedJobs.size() == 0) {
                bot.sendMessage(channel, "Could not find any blocks matching " + blockQuery);
            } else if(matchedJobs.size() > 1) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for(JobKey jobKey : matchedJobs) {
                    JobDetail job = scheduler.getJobDetail(jobKey);
                    if(!first) sb.append(", ");
                    sb.append(job.getJobDataMap().getString(SchedulerConstants.TSDTV_BLOCK_NAME_FIELD));
                    first = false;
                }
                bot.sendMessage(channel, "Found multiple blocks matching \"" + blockQuery + "\": " + sb.toString());
            } else {
                JobDetail job = scheduler.getJobDetail(matchedJobs.get(0));
                TSDTVBlock blockInfo = new TSDTVBlock(job.getJobDataMap());

                try {
                    prepareScheduledBlock(blockInfo, -1);
                } catch (SQLException e) {
                    log.error("Error preparing scheduled block", e);
                }
            }

        } catch (SchedulerException e) {
            bot.sendMessage(channel, "(Error retrieving scheduled info)");
            log.error("Error getting scheduled info", e);
        }
    }

    public void printSchedule(String channel, boolean todayOnly) {

        HashMap<String, String> metadata;

        if(runningStream != null) {
            bot.sendMessage(channel, "NOW PLAYING: " + runningStream.getMovie().video.toString());
        }

        if(!queue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("On deck: ");
            boolean first = true;
            for(TSDTVQueueItem program : queue) {
                if(program.video.isBroadcastable()) {
                    TSDTVShow show = ((TSDTVEpisode)program.video).getShow();
                    if(!first)
                        sb.append(", ");
                    sb.append(show.getPrettyName());
                    first = false;
                }
            }
            bot.sendMessage(channel, sb.toString());
        }

        try {

            GregorianCalendar endOfToday = new GregorianCalendar();
            endOfToday.setTimeZone(timeZone);
            endOfToday.set(Calendar.HOUR_OF_DAY, dayBoundaryHour);
            if(endOfToday.get(Calendar.HOUR_OF_DAY) >= dayBoundaryHour)
                endOfToday.add(Calendar.DATE, 1);

            Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.<JobKey>groupEquals(SchedulerConstants.TSDTV_GROUP_ID));
            TreeMap<Date, JobDetail> jobMap = new TreeMap<>();
            for(JobKey key : keys) {
                List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(key);
                if(!triggers.isEmpty()) {
                    Date nextFireTime = triggers.get(0).getNextFireTime();
                    if((!todayOnly) || nextFireTime.before(endOfToday.getTime()))
                        jobMap.put(nextFireTime, scheduler.getJobDetail(key));
                }
            }

            if(!jobMap.isEmpty()) {
                StringBuilder sb;
                SimpleDateFormat sdf;
                if(todayOnly)
                    sdf = new SimpleDateFormat("HH:mm a z");
                else
                    sdf = new SimpleDateFormat("EEE HH:mm a z");
                sdf.setTimeZone(timeZone);
                for(Date d : jobMap.keySet()) {
                    sb = new StringBuilder();
                    sb.append(sdf.format(d)).append(" -- ");
                    JobDetail job = jobMap.get(d);
                    TSDTVBlock blockInfo = new TSDTVBlock(job.getJobDataMap());
                    sb.append(blockInfo.name).append(": ");
                    boolean first = true;
                    for(String s : blockInfo.scheduleParts) {
                        if(s.startsWith(".")) continue;
                        if(sb.toString().contains(s)) continue;
                        if(!first) sb.append(", ");
                        sb.append(s);
                        first = false;
                    }
                    bot.sendMessage(channel, sb.toString());
                }
            }

        } catch (SchedulerException e) {
            bot.sendMessage(channel, "(Error retrieving scheduled info)");
            log.error("Error getting scheduled info", e);
        }
    }

    public void buildSchedule() {
        try {
            log.info("Building TSDTV schedule...");
            scheduler.pauseAll();
            Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.<JobKey>groupEquals(SchedulerConstants.TSDTV_GROUP_ID));
            scheduler.deleteJobs(new LinkedList<>(keys));

            JobDetail job;
            CronTrigger cronTrigger;

            FileInputStream schedule = new FileInputStream(new File(scheduleLoc));
            try(BufferedReader br = new BufferedReader(new InputStreamReader(schedule))) {
                String line = null;
                while((line = br.readLine()) != null) {
                    if(line.startsWith("BLOCK")) {
                        String blockName = line.substring(line.indexOf("=") + 1);

                        String blockLine = br.readLine();
                        String blockId = blockLine.substring(blockLine.indexOf("=") + 1);

                        String quartz = br.readLine();

                        LinkedList<String> shows = new LinkedList<>();
                        while(!(line = br.readLine()).equals("ENDBLOCK")) {
                            if(!line.startsWith("#"))
                                shows.add(line);
                        }

                        String prettySchedule = StringUtils.join(shows, SchedulerConstants.TSDTV_BLOCK_SCHEDULE_DELIMITER);

                        job = newJob(TSDTVBlockJob.class)
                                .withIdentity(blockName, SchedulerConstants.TSDTV_GROUP_ID)
                                .usingJobData(SchedulerConstants.TSDTV_BLOCK_SCHEDULE_FIELD, prettySchedule)
                                .usingJobData(SchedulerConstants.TSDTV_BLOCK_NAME_FIELD, blockName)
                                .usingJobData(SchedulerConstants.TSDTV_BLOCK_ID_FIELD, blockId)
                                .build();

                        cronTrigger = newTrigger()
                                .withSchedule(cronSchedule(quartz))
                                .build();

                        scheduler.scheduleJob(job, cronTrigger);
                    }
                }
            } catch (Exception e) {
                log.error("Error reading TSDTV schedule", e);
            }

            scheduler.resumeAll();

        } catch (Exception e) {
            log.error("Error building TSDTV schedule", e);
        }
    }

    public void finishStream(boolean playNext) {
        runningStream = null;
        if(playNext && !queue.isEmpty()) {
            play(queue.pop());
        } else {
            queue.clear();
        }
    }

    public StreamState getState() throws NoStreamRunningException {
        if(runningStream == null) {
            throw new NoStreamRunningException();
        }
        return runningStream.getStreamState();
    }

    private int getCurrentEpisode(TSDTVShow show) throws SQLException {
        Connection dbConn = connectionProvider.get();
        String q = String.format("select currentEpisode from %s where name = ?", showsTable);
        try(PreparedStatement ps = dbConn.prepareStatement(q)) {
            ps.setString(1, show.getRawName());
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    return result.getInt("currentEpisode");
                }
            }
        }
        return -1;
    }

    public int getViewerCount() {

        //TODO: extend this to match connections with chat users

        String matchString = "^nginx.*192\\.168\\.1\\.100:1935->.*\\(ESTABLISHED\\)$";
        int viewerCount = 0;

        try {
            ProcessBuilder pb = new ProcessBuilder("lsof", "-n", "-i", "tcp:1935");
            Process p = pb.start();
            p.waitFor();
            InputStream out = p.getInputStream();
            InputStreamReader reader = new InputStreamReader(out);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while( (line = br.readLine()) != null ) {
                if(line.trim().matches(matchString)) {
                    viewerCount++;
                }
            }
        } catch (IOException | InterruptedException e) {
            return -1;
        }

        return viewerCount;
    }

    /**
     * Produces links to the TSDTV web player and direct stream (optional)
     * @param includeDirect include the direct stream link
     * @return the links
     */
    public String getLinks(boolean includeDirect) {
        StringBuilder sb = new StringBuilder();
        sb.append(serverUrl).append("/tsdtv");
        if(includeDirect) {
            sb.append(" -- DIRECT: ").append(tsdtvDirect);
        }
        sb.append(" -- CHANNEL: ").append(StringUtils.join(tsdtvChannels.toArray(), ", "));
        return sb.toString();
    }

    /**
     * Determines whether or not a video has a stream with subtitles
     * @param file the video in question
     * @return true if the video has subtitles
     */
    private boolean hasSubtitles(File file) {
        try {
            FileAnalysis analysis = processor.analyzeFile(file);
            return analysis.getStreamsByType().containsKey(StreamType.SUBTITLE);
        } catch (Exception e) {
            log.error("Error finding subtitles for {}", file, e);
        }
        return false;
    }

    /**
     * Finds the starting time for a video being played or added to the queue
     * @return the time the video will start
     */
    private Date getStartDateForQueueItem() {
        if(queue.isEmpty()) {
            if(isRunning())
                return runningStream.getMovie().endTime;
            else
                return new Date();
        } else {
            return queue.getLast().endTime;
        }
    }

    private void setEpisodeNumber(TSDTVShow show, int num) throws SQLException {
        Connection dbConn = connectionProvider.get();
        String update = String.format("update %s set currentEpisode = ? where name = ?", showsTable);
        try(PreparedStatement ps = dbConn.prepareStatement(update)) {
            ps.setInt(1, num);
            ps.setString(2, show.getRawName());
            ps.executeUpdate();
        } catch (SQLException sqle) {
            log.error("Error setting episode number", sqle);
            throw sqle;
        }
    }
    
    void broadcast(String message) {
        for(String channel : tsdtvChannels) {
            bot.sendMessage(channel, message);
        }
    }

    public static class TSDTVBlock {
        public String id;
        public String name;
        public String[] scheduleParts;

        @Deprecated
        private TSDTVBlock() {}

        public TSDTVBlock(JobDataMap jobDataMap) {
            this.name = jobDataMap.getString(SchedulerConstants.TSDTV_BLOCK_NAME_FIELD);
            this.id = jobDataMap.getString(SchedulerConstants.TSDTV_BLOCK_ID_FIELD);
            this.scheduleParts = jobDataMap.getString(SchedulerConstants.TSDTV_BLOCK_SCHEDULE_FIELD)
                    .split(SchedulerConstants.TSDTV_BLOCK_SCHEDULE_DELIMITER);
        }
    }

}
