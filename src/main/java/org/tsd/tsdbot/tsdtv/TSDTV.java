package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.database.DBConnectionProvider;
import org.tsd.tsdbot.database.Persistable;
import org.tsd.tsdbot.scheduled.SchedulerConstants;
import org.tsd.tsdbot.tsdtv.model.*;
import org.tsd.tsdbot.util.FuzzyLogic;

import javax.naming.AuthenticationException;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.tsd.tsdbot.util.IRCUtil.*;

/**
 * Created by Joe on 3/9/14.
 */
@Singleton
public class TSDTV implements Persistable {

    private static Logger logger = LoggerFactory.getLogger(TSDTV.class);

    private static final int dayBoundaryHour = 4; // 4:00 AM
    private static final TimeZone timeZone = TimeZone.getTimeZone("America/New_York");


    private TSDBot bot;

    private TSDTVLibrary library;

    private Random random;
    private DBConnectionProvider connectionProvider;
    private InjectableStreamFactory streamFactory;
    private Scheduler scheduler;
    private String scheduleLoc;
    private String serverUrl;
    private String ffmpegExec;
    private String tsdtvDirect;

    private LinkedList<TSDTVQueueItem> queue = new LinkedList<>(); // file paths
    private TSDTVStream runningStream;

    @Inject
    public TSDTV(TSDBot bot, TSDTVLibrary library, Properties prop, Scheduler scheduler,
                 DBConnectionProvider connectionProvider, InjectableStreamFactory streamFactory,
                 Random random, @Named("serverUrl") String serverUrl,
                 @Named("ffmpegExec") String ffmpegExec, @Named("tsdtvDirect") String tsdtvDirect) throws SQLException {
        this.bot = bot;
        this.random = random;
        this.library = library;
        this.scheduleLoc = prop.getProperty("tsdtv.schedule");
        this.scheduler = scheduler;
        this.connectionProvider = connectionProvider;
        this.streamFactory = streamFactory;
        this.serverUrl = serverUrl;
        this.ffmpegExec = ffmpegExec;
        this.tsdtvDirect = tsdtvDirect;
        initDB();
        buildSchedule();
    }

    @Override
    public void initDB() throws SQLException {
        logger.info("Initializing TSDTV database");

        Connection connection = connectionProvider.get();

        // load new shows
        String showsTable = "TSDTV_SHOW";
        String createShows = String.format("create table if not exists %s (" +
                "id int auto_increment," +
                "name varchar," +
                "currentEpisode int," +
                "primary key (id))", showsTable);
        try(PreparedStatement ps = connection.prepareStatement(createShows)) {
            logger.info("TSDTV_SHOW: {}", createShows);
            ps.executeUpdate();
        }

        logger.info("Building TSDTV_SHOW table...");
        for(TSDTVShow show : library.getAllShows()) {
            String q = String.format("select count(*) from %s where name = '%s'", showsTable, show.getRawName());
            try(PreparedStatement ps = connection.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
                result.next();
                if(result.getInt(1) == 0) { // show does not exist in db, add it
                    logger.info("Could not find show {} in DB, adding...", show.getRawName());
                    String insertShow = String.format(
                            "insert into %s (name, currentEpisode) values ('%s',1)",
                            showsTable,
                            show.getRawName());
                    try(PreparedStatement ps1 = connection.prepareCall(insertShow)) {
                        ps1.executeUpdate();
                    }
                } else {
                    logger.info("Show {} already exists in DB, skipping...", show.getRawName());
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
        if(hasSubtitles(program.video))
            videoFilter.append(",subtitles=").append(program.video.getFile().getAbsolutePath());

        runningStream = streamFactory.newStream(videoFilter.toString(), program);
        runningStream.start();

        if(program.scheduled && program.video.getEpisodeNumber() > 0) {
            try {
                TSDTVShow show = ((TSDTVEpisode)program.video).getShow();
                Connection dbConn = connectionProvider.get();
                String update = "update TSDTV_SHOW set currentEpisode = ? where name = ?";
                try(PreparedStatement ps = dbConn.prepareStatement(update)) {
                    // loop to episode 1 if we're playing the last episode of the show
                    int newEpNumber = (show.getAllEpisodes().size() <= program.video.getEpisodeNumber()) ? 1 : program.video.getEpisodeNumber()+1;
                    ps.setInt(1, newEpNumber);
                    ps.setString(2, show.getRawName());
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                logger.error("Error updating show episode number", e);
            }
        }

        if(program.video.isBroadcastable()) {
            HashMap<String,String> metadata = program.video.getMetadata(ffmpegExec);
            String artist = metadata.get(TSDTVConstants.METADATA_ARTIST_FIELD);
            String title = metadata.get(TSDTVConstants.METADATA_TITLE_FIELD);

            StringBuilder sb = new StringBuilder();
            sb.append(color("[TSDTV]", IRCColor.blue)).append(" [NOW PLAYING]: ");
            if(artist == null || title == null)
                sb.append(program.video.toString());
            else
                sb.append(artist).append(": ").append(title);
            sb.append(" -- ").append(getLinks(false));

            bot.broadcast(sb.toString());
        }
    }

    /**
     * method to play a movie from the chat. returns true if playing immediately, false if queued
     */
    public boolean playFromChat(TSDTVEpisode episode, String ident) throws ShowNotFoundException {
        TSDTVQueueItem program = new TSDTVQueueItem(episode, null, false, getStartDateForQueueItem(), ffmpegExec, ident);
        if(isRunning()) {
            queue.addLast(program);
            return false;
        } else {
            play(program);
            return true;
        }
    }

    /**
     * method to play a movie from the webpage catalog
     */
    public void playFromCatalog(TSDTVEpisode episode, String ipAddr) throws ShowNotFoundException {
        TSDTVQueueItem program = new TSDTVQueueItem(episode, null, false, getStartDateForQueueItem(), ffmpegExec, ipAddr);
        if(isRunning()) {
            queue.addLast(program);
            bot.broadcast(color("[TSDTV]", IRCColor.blue)
                    + " A show has been enqueued via web: " + episode.getShow().getPrettyName() + " - " + episode.getPrettyName());
        } else {
            play(program);
        }
    }

    public void prepareScheduledBlock(TSDTVBlock blockInfo, int offset) throws SQLException {

        logger.info("Preparing TSDTV block: {} with offset {}", blockInfo.name, offset);

        if(runningStream != null) {
            runningStream.interrupt(); // end running stream
            logger.info("Ended currently running stream");
        }

        queue.clear();

        boolean scheduled = (offset == 0);

        // prepare the block intro if it exists
        TSDTVFiller blockIntro = library.getFiller(FillerType.block_intro, blockInfo.id);
        if(blockIntro != null) {
            queue.addLast(new TSDTVQueueItem(blockIntro, blockInfo, scheduled, getStartDateForQueueItem(), ffmpegExec, null));
        }

        // use dynamic map to get correct episode numbers for repeating shows
        // use offset to handle replays/reruns
        HashMap<TSDTVShow, Integer> episodeNums = new HashMap<>(); // show -> episode num
        for(String showName : blockInfo.scheduleParts) {

            HashMap<String, String> metadata = null;

            FillerType fillerType = FillerType.fromSchedule(showName);
            if(fillerType != null) {
                // this is filler, e.g. bump or commercial
                TSDTVFiller filler = library.getFiller(fillerType, null);
                if(filler != null) {
                    queue.addLast(new TSDTVQueueItem(filler, blockInfo, scheduled, getStartDateForQueueItem(), ffmpegExec, null));
                    logger.info("Added {} to queue", filler.getFile().getAbsolutePath());
                } else {
                    logger.error("Could not find any filler of type {}", fillerType);
                }
            } else {

                // this isn't filler, it must be a TSDTVShow
                TSDTVShow show;
                try {
                    show = library.getShow(showName);
                } catch (ShowNotFoundException e) {
                    logger.error("Could not find show", e);
                    continue;
                }

                // add the intro for this show, if it exists
                TSDTVFiller intro = library.getFiller(FillerType.show_intro, showName);
                if(intro != null) {
                    queue.addLast(new TSDTVQueueItem(intro, blockInfo, scheduled, getStartDateForQueueItem(), ffmpegExec, null));
                }

                int occurrences = Collections.frequency(Arrays.asList(blockInfo.scheduleParts), showName);
                int episodeNum;
                if(!episodeNums.containsKey(show)) {
                    // this show hasn't appeared in the block yet -- get current episode num from DB
                    episodeNum = getCurrentEpisode(show) + (occurrences * offset);
                    if(episodeNum > 0)
                        episodeNums.put(show, episodeNum);
                    else
                        logger.error("Could not find current episode for {}", show);
                } else {
                    // this show has appeared in the block -- increment episode num
                    if(episodeNums.get(show)+1 > show.getAllEpisodes().size())
                        episodeNum = 1; // wrap if we reached the end
                    else
                        episodeNum = episodeNums.get(show)+1;
                    episodeNums.put(show,episodeNum);
                }

                logger.info("Looking for episode {} of {}", episodeNum, show.getRawName());
                TSDTVEpisode episode;
                try {
                    episode = show.getEpisode(episodeNum);
                } catch (ShowNotFoundException e) {
                    logger.error("Could not find episode", e);
                    continue;
                }

                queue.addLast(new TSDTVQueueItem(episode, blockInfo, scheduled, getStartDateForQueueItem(), ffmpegExec, null));

                logger.info("Added {} to queue", episode.getRawName());

            }
        }

        // prepare the block outro if it exists
        TSDTVFiller blockOutro = library.getFiller(FillerType.block_outro, blockInfo.id);
        if(blockOutro != null) {
            queue.addLast(new TSDTVQueueItem(blockOutro, blockInfo, scheduled, getStartDateForQueueItem(), ffmpegExec, null));
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

        if(blockIntro != null) {
            // there's a block intro playing, link people to the stream while it plays
            bot.broadcast(getLinks(false));
        }

        if(!queue.isEmpty())
            play(queue.pop());
        else
            logger.error("Could not find any shows for block...");
    }

    public void prepareBlockReplay(String channel, String blockQuery) {

        logger.info("Preparing TSDTV block rerun: {}", blockQuery);

        if(runningStream != null) {
            bot.sendMessage(channel, "There is already a stream running, please wait for it to" +
                    " finish  before starting a block rerun");
            return;
        }

        try {
            Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.<JobKey>groupEquals(SchedulerConstants.TSDTV_GROUP_ID));
            LinkedList<JobKey> matchedJobs = FuzzyLogic.fuzzySubset(blockQuery, new LinkedList<>(keys), new FuzzyLogic.FuzzyVisitor<JobKey>() {
                @Override
                public String visit(JobKey o1) {
                    try {
                        JobDetail job = scheduler.getJobDetail(o1);
                        return job.getJobDataMap().getString(SchedulerConstants.TSDTV_BLOCK_NAME_FIELD);
                    } catch (SchedulerException e) {
                        logger.error("Error getting job for key {}", o1.getName());
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
                    logger.error("Error preparing scheduled block", e);
                }
            }

        } catch (SchedulerException e) {
            bot.sendMessage(channel, "(Error retrieving scheduled info)");
            logger.error("Error getting scheduled info", e);
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
                StringBuilder sb = null;
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
            logger.error("Error getting scheduled info", e);
        }
    }

    public void buildSchedule() {
        try {
            logger.info("Building TSDTV schedule...");
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
                logger.error("Error reading TSDTV schedule", e);
            }

            scheduler.resumeAll();

        } catch (Exception e) {
            logger.error("Error building TSDTV schedule", e);
        }
    }

    public boolean authorized(String userId) throws NoStreamRunningException {
        if(runningStream != null)
            return userId.equalsIgnoreCase(runningStream.getMovie().owner);
        else
            throw new NoStreamRunningException();
    }

    public void kill(boolean playNext) throws NoStreamRunningException {
        if(runningStream != null)
            runningStream.kill(playNext);
        else
            throw new NoStreamRunningException();
    }

    public void pause() throws NoStreamRunningException, IllegalStateException {
        if(runningStream != null)
            runningStream.pauseStream();
        else
            throw new NoStreamRunningException();
    }

    public void unpause() throws NoStreamRunningException, IllegalStateException {
        if(runningStream != null)
            runningStream.resumeStream();
        else
            throw new NoStreamRunningException();
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
        if(runningStream == null)
            throw new NoStreamRunningException();
        return runningStream.getStreamState();
    }

    private int getCurrentEpisode(TSDTVShow show) throws SQLException {
        Connection dbConn = connectionProvider.get();
        String q = String.format("select currentEpisode from TSDTV_SHOW where name = '%s'", show.getRawName());
        try(PreparedStatement ps = dbConn.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            if(result.next()) {
                return result.getInt("currentEpisode");
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
                if(line.trim().matches(matchString))
                    viewerCount++;
            }
        } catch (IOException | InterruptedException e) {
            return -1;
        }

        return viewerCount;
    }

    public String getLinks(boolean includeDirect) {
        StringBuilder sb = new StringBuilder();
        sb.append(serverUrl).append("/tsdtv");
        if(includeDirect)
            sb.append(" -- DIRECT: ").append(tsdtvDirect);
        return sb.toString();
    }

    private HashMap<Integer, StreamType> getVideoStreams(Streamable video) {

        Pattern trackPattern = Pattern.compile("^Track ID\\s+(\\d+):\\s+(\\w+)\\s+\\(.*\\)$", Pattern.DOTALL);
        HashMap<Integer, StreamType> streams = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder("mkvmerge", "-i", video.getFile().getAbsolutePath());
            Process p = pb.start();
            p.waitFor();
            InputStream out = p.getInputStream();
            InputStreamReader reader = new InputStreamReader(out);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while( (line = br.readLine()) != null ) {
                logger.info(line);
                if(line.contains("Chapters")) break;
                if(line.contains("Track ID")) {
                    Matcher m = trackPattern.matcher(line);
                    while(m.find()) {
                        Integer streamNo = Integer.parseInt(m.group(1));
                        StreamType streamType = StreamType.fromString(m.group(2));
                        streams.put(streamNo, streamType);
                    }
                }
            }

        } catch (InterruptedException | IOException e) {
            logger.error(e.getMessage());
        }

        return streams;
    }

    private boolean hasSubtitles(Streamable video) {
        HashMap<Integer, StreamType> streams = getVideoStreams(video);
        for(Integer streamNum : streams.keySet()) {
            if(streams.get(streamNum).equals(StreamType.SUBTITLES)) {
                return true;
            }
        }
        return false;
    }

    private Date getStartDateForQueueItem() {
        if(isRunning()) {
            if(!queue.isEmpty()) {
                return queue.getLast().endTime;
            } else {
                return runningStream.getMovie().endTime;
            }
        } else {
            return new Date();
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

    static enum StreamType {
        VIDEO,
        AUDIO,
        SUBTITLES;

        public static StreamType fromString(String s) {
            for(StreamType type : StreamType.values()) {
                if(type.toString().compareToIgnoreCase(s) == 0)
                    return type;
            }
            return null;
        }
    }
}
