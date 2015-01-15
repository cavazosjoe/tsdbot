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
import org.tsd.tsdbot.util.FuzzyLogic;
import org.tsd.tsdbot.util.IRCUtil;
import org.tsd.tsdbot.util.TSDTVUtil;

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

/**
 * Created by Joe on 3/9/14.
 */
@Singleton
public class TSDTV implements Persistable {

    private static Logger logger = LoggerFactory.getLogger(TSDTV.class);


    private static final int dayBoundaryHour = 4; // 4:00 AM
    private static final TimeZone timeZone = TimeZone.getTimeZone("America/New_York");

    private TSDBot bot;

    private DBConnectionProvider connectionProvider;
    private Random random;
    private InjectableStreamFactory streamFactory;
    private Scheduler scheduler;
    private String catalogPath;
    private String scheduleLoc;
    private String serverUrl;

    private LinkedList<TSDTVProgram> queue = new LinkedList<>(); // file paths

    private ThreadStream runningStream;

    @Inject
    public TSDTV(TSDBot bot, Properties prop, Scheduler scheduler,
                 DBConnectionProvider connectionProvider, InjectableStreamFactory streamFactory,
                 Random random, @Named("serverUrl") String serverUrl) throws SQLException {
        this.bot = bot;
        this.random = random;
        this.catalogPath = prop.getProperty("tsdtv.catalog");
        this.scheduleLoc = prop.getProperty("tsdtv.schedule");
        this.scheduler = scheduler;
        this.connectionProvider = connectionProvider;
        this.streamFactory = streamFactory;
        this.serverUrl = serverUrl;
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

        File catalogDir = new File(catalogPath);
        logger.info("Building TSDTV_SHOW table from directory {}", catalogDir.getAbsolutePath());
        for(File f : catalogDir.listFiles()) {
            if(f.isDirectory()) {
                String q = String.format("select count(*) from %s where name = '%s'", showsTable, f.getName());
                try(PreparedStatement ps = connection.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
                    result.next();
                    if(result.getInt(1) == 0) { // show does not exist in db, add it
                        logger.info("Could not find show {} in DB, adding...", f.getName());
                        String insertShow = String.format(
                                "insert into %s (name, currentEpisode) values ('%s',1)",
                                showsTable,
                                f.getName());
                        try(PreparedStatement ps1 = connection.prepareCall(insertShow)) {
                            ps1.executeUpdate();
                        }
                    } else {
                        logger.info("Show {} already exists in DB, skipping...", f.getName());
                    }
                }
            }
        }
    }

    public File getCatalogDir() {
        return new File(catalogPath);
    }

    public File getShowDir(String show) throws ShowNotFoundException {
        return getFuzzyShow(show);
    }

    public ShowInfo getPrevAndNextEpisodeNums(String showQuery) throws ShowNotFoundException, SQLException {
        File show = getFuzzyShow(showQuery);
        int nextEpisode = getCurrentEpisode(show.getName());
        if(nextEpisode > getNumberOfEpisodes(show.getName()))
            nextEpisode = 1;

        int prevEpisode = getCurrentEpisode(show.getName()) - 1;
        if(prevEpisode < 1)
            prevEpisode = getNumberOfEpisodes(show.getName());

        return new ShowInfo(show.getName(), prevEpisode, nextEpisode);
    }

    public boolean isRunning() {
        return runningStream != null;
    }

    public void catalog(String requester, String subdir) throws Exception {
        File printingDir;
        if(subdir == null)
            printingDir = new File(catalogPath);
        else
            printingDir = getFuzzyShow(subdir);

        boolean first = true;
        StringBuilder catalogBuilder = new StringBuilder();
        for(File f : printingDir.listFiles()) {
            if(!first)catalogBuilder.append(" || ");
            catalogBuilder.append(f.getName());
            if(f.isDirectory()) catalogBuilder.append(" (DIR)");
            first = false;
        }

        bot.sendMessages(requester, IRCUtil.splitLongString(catalogBuilder.toString()));

    }

    private void play(TSDTVProgram program) {

        // determine if we have subtitles for this
        StringBuilder videoFilter = new StringBuilder();
        videoFilter.append("yadif");
        if(hasSubtitles(program.filePath))
            videoFilter.append(",subtitles=").append(program.filePath);

        TSDTVStream stream = streamFactory.newStream(videoFilter.toString(), program.filePath);
        Thread thread = new Thread(stream);
        runningStream = new ThreadStream(thread, stream);
        runningStream.begin();

        if(program.show != null && program.episodeNum > 0) {
            try {
                Connection dbConn = connectionProvider.get();
                String update = "update TSDTV_SHOW set currentEpisode = ? where name = ?";
                try(PreparedStatement ps = dbConn.prepareStatement(update)) {
                    // loop to episode 1 if we're playing the last episode of the show
                    int newEpNumber = (getNumberOfEpisodes(program.show) <= program.episodeNum) ? 1 : program.episodeNum+1;
                    ps.setInt(1, newEpNumber);
                    ps.setString(2, program.show);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                logger.error("Error updating show episode number", e);
            }
        }

        if(!program.show.startsWith(".")) { // skip commercials and bumps
            HashMap<String,String> metadata = getVideoMetadata(program.filePath);
            String artist = metadata.get(TSDTVConstants.METADATA_ARTIST_FIELD);
            if(artist == null) artist = program.show;
            String title = metadata.get(TSDTVConstants.METADATA_TITLE_FIELD);
            if(title == null) title = program.filePath.substring(program.filePath.lastIndexOf("/")+1);

            String msg = "[TSDTV] NOW PLAYING: " + artist + ": " + title + " -- " + getLinks(false);
            bot.broadcast(msg.replaceAll("_"," "));
        }
    }

    public void prepareOnDemand(String channel, String dir, String query) throws Exception {

        File searchingDir = getFuzzyShow(dir);
        String show = searchingDir.getName();

        LinkedList<File> matchedFiles = new LinkedList<>();
        if(TSDTVConstants.RANDOM_QUERY.equals(query)) {
            matchedFiles.add(getRandomFileFromDirectory(searchingDir));
        } else {
            matchedFiles = FuzzyLogic.fuzzySubset(query, Arrays.asList(searchingDir.listFiles()), new FuzzyLogic.FuzzyVisitor<File>() {
                @Override
                public String visit(File o1) {
                    return o1.getName();
                }
            });
        }

        if(matchedFiles.size() == 0) {
            throw new Exception("Could not find movie that matches " + query);
        } else if(matchedFiles.size() > 1) {
            StringBuilder ex = new StringBuilder();
            ex.append("Found multiple matching movies: ");
            for(File match : matchedFiles)
                ex.append(match.getName()).append(" ");
            throw new Exception(ex.toString());
        }

        TSDTVProgram program = new TSDTVProgram(matchedFiles.get(0).getAbsolutePath(), show);
        if(isRunning()) {
            queue.addLast(program);
            bot.sendMessage(channel, "There is already a stream running. Your show has been enqueued");
        } else play(program);

    }

    /**
     * method to play a movie from the webpage catalog
     * returns TRUE if it will play immediately, FALSE if it's queued
     */
    public boolean playFromCatalog(String fileName, String show) throws ShowNotFoundException {
        File showDir = getShowDir(show);
        String filePath = showDir.getAbsolutePath() + "/" + fileName;
        TSDTVProgram program = new TSDTVProgram(filePath, show);
        if(isRunning()) {
            queue.addLast(program);
            bot.broadcast("[TSDTV] A show has been enqueued via web: " + show + " - " + fileName);
            return false;
        } else {
            play(program);
            return true;
        }
    }

    public void prepareScheduledBlock(TSDTVBlock blockInfo, int offset) throws SQLException {

        logger.info("Preparing TSDTV block: {} with offset {}", blockInfo.name, offset);

        if(runningStream != null) {
            runningStream.kill(); // end running stream
            logger.info("Ended currently running stream");
        }

        queue.clear();

        // prepare the block intro if it exists
        String blockIntro = getBlockIntro(blockInfo.id);
        if(blockIntro != null)
            queue.addLast(new TSDTVProgram(blockIntro, TSDTVConstants.INTRO_DIR_NAME));

        // use dynamic map to get correct episode numbers for repeating shows
        // use offset to handle replays/reruns
        HashMap<String, Integer> episodeNums = new HashMap<>(); // show -> episode num
        for(String show : blockInfo.scheduleParts) {

            if(show.startsWith(".")) { // commercial or bump, grab random

                File showDir = new File(catalogPath + "/" + show);
                String showPath = getRandomFilePathFromDirectory(showDir);
                if(showPath != null) {
                    queue.addLast(new TSDTVProgram(showPath, show));
                    logger.info("Added {} to queue", showPath);
                } else {
                    logger.error("Could not find any shows in {}", showDir.getAbsolutePath());
                }

            } else {

                String introPath = getShowIntro(show);
                if(introPath != null) {
                    queue.addLast(new TSDTVProgram(introPath, TSDTVConstants.INTRO_DIR_NAME));
                }

                int occurrences = Collections.frequency(Arrays.asList(blockInfo.scheduleParts), show);
                int episodeNum = 0;
                if(!episodeNums.containsKey(show)) {
                    // this show hasn't appeared in the block yet -- get current episode num from DB
                    episodeNum = getCurrentEpisode(show) + (occurrences * offset);
                    if(episodeNum > 0)
                        episodeNums.put(show, episodeNum);
                    else
                        logger.error("Could not find current episode for {}", show);
                } else {
                    // this show has appeared in the block -- increment episode num
                    if(episodeNums.get(show)+1 > getNumberOfEpisodes(show)) episodeNum = 1; // wrap if we reached the end
                    else episodeNum = episodeNums.get(show)+1;
                    episodeNums.put(show,episodeNum);
                }

                logger.info("Looking for episode {} of {}", episodeNum, show);
                String episodePath = getEpisode(show, episodeNum);
                if(episodePath != null) {
                    if(offset == 0)
                        queue.addLast(new TSDTVProgram(episodePath, show, episodeNum));
                    else
                        queue.addLast(new TSDTVProgram(episodePath, show)); // don't worry about ep if it's a replay
                    logger.info("Added {} to queue", episodePath);
                } else {
                    logger.error("Could not retrieve episode {} of {}", episodeNum, show);
                }

            }
        }

        StringBuilder broadcastBuilder = new StringBuilder();
        broadcastBuilder.append("[TSDTV] \"").append(blockInfo.name).append("\" block now starting. Lined up: ");

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

        if(!queue.isEmpty()) play(queue.pop());
        else logger.error("Could not find any shows for block...");
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
            metadata = getVideoMetadata(runningStream.stream.getPathToMovie());
            String artist = metadata.get(TSDTVConstants.METADATA_ARTIST_FIELD);
            String title = metadata.get(TSDTVConstants.METADATA_TITLE_FIELD);

            String np;
            if(artist == null || title == null) np = runningStream.stream.getMovieName();
            else np = artist + " - " + title;

            bot.sendMessage(channel, "NOW PLAYING: " + np);
        }

        if(!queue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("On deck: ");
            boolean first = true;
            for(TSDTVProgram program : queue) {
                if(program.show.startsWith(".")) continue;
                if(!first) sb.append(", ");
                sb.append(program.show);
                first = false;
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

    public void kill() {
        logger.info("Received kill signal...");
        if(runningStream != null) {
            runningStream.kill();
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

    private File getFuzzyShow(String query) throws ShowNotFoundException {
        // return the File object, use it to getName or getPath
        List<File> matchingDirs = FuzzyLogic.fuzzySubset(
                query,
                Arrays.asList(new File(catalogPath).listFiles()),
                new FuzzyLogic.FuzzyVisitor<File>() {
                    @Override
                    public String visit(File o1) {
                        return o1.getName();
                    }
        });

        if(matchingDirs.size() == 0)
            throw new ShowNotFoundException("Could not find directory matching \"" + query + "\"");
        else if(matchingDirs.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("Found multiple directories matching for \"").append(query).append("\":");
            for(File f : matchingDirs)
                sb.append(" ").append(f.getName());
            throw new ShowNotFoundException(sb.toString());
        } else {
            return matchingDirs.get(0);
        }
    }

    private String getEpisode(String show, int episodeNumber) {
        File showDir = new File(catalogPath + "/" + show);
        java.util.regex.Matcher epNumMatcher;
        if(showDir.exists()) {
            for(File f : showDir.listFiles()) try {
                int epNum = TSDTVUtil.getEpisodeNumberFromFilename(f.getName());
                if(epNum == episodeNumber) {
                    return f.getAbsolutePath();
                }
            } catch (Exception e) {
                logger.warn("Failed to parse episode number from file {}, skipping...", f.getName());
            }
        } else {
            logger.error("Could not find show directory: {}", catalogPath + "/" + show);
        }

        return null;
    }

    private String getShowIntro(String show) {
        File introDir = new File(catalogPath + "/" + show + "/" + TSDTVConstants.INTRO_DIR_NAME);
        return getRandomFilePathFromDirectory(introDir);
    }

    private String getBlockIntro(String blockId) {
        File introDir = new File(catalogPath + "/"
                + TSDTVConstants.BLOCKS_DIR_NAME + "/" + blockId + "/" + TSDTVConstants.INTRO_DIR_NAME);
        return getRandomFilePathFromDirectory(introDir);
    }

    private File getRandomFileFromDirectory(File dir) {
        if(dir.exists()) {
            File[] files = dir.listFiles();
            if(files == null || files.length == 0)
                return null;
            return files[random.nextInt(files.length)];
        }
        return null;
    }

    private String getRandomFilePathFromDirectory(File dir) {
        File f = getRandomFileFromDirectory(dir);
        if(f == null) return null;
        else return f.getAbsolutePath();
    }

    private int getCurrentEpisode(String show) throws SQLException {
        Connection dbConn = connectionProvider.get();
        String q = String.format("select currentEpisode from TSDTV_SHOW where name = '%s'", show);
        try(PreparedStatement ps = dbConn.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            if(result.next()) {
                return result.getInt("currentEpisode");
            }
        }
        return -1;
    }

    public int getViewerCount() {

        //TODO: extend this to match connections with chat users

        String matchString = "^ffserver.*192\\.168\\.1\\.100:8090->.*\\(ESTABLISHED\\)$";
        int viewerCount = 0;

        try {
            ProcessBuilder pb = new ProcessBuilder("lsof", "-n", "-i", "tcp:8090");
            Process p = pb.start();
            p.waitFor();
            InputStream out = p.getInputStream();
            InputStreamReader reader = new InputStreamReader(out);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while( (line = br.readLine()) != null ) {
                logger.info(line);
                if(line.trim().matches(matchString))
                    viewerCount++;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error retrieving viewer count", e);
            return -1;
        }

        return viewerCount;
    }

    public String getLinks(boolean includeVlc) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://irc.teamschoolyd.org/tsdtv.html");
        if(includeVlc)
            sb.append(" -- VLC: http://irc.teamschoolyd.org:8090/tsdtv.flv");
        return sb.toString();
    }

    private HashMap<Integer, StreamType> getVideoStreams(String moviePath) {

        Pattern trackPattern = Pattern.compile("^Track ID\\s+(\\d+):\\s+(\\w+)\\s+\\(.*\\)$", Pattern.DOTALL);
        HashMap<Integer, StreamType> streams = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder("mkvmerge", "-i", moviePath);
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

    private HashMap<String, String> getVideoMetadata(String moviePath) {

        HashMap<String, String> metadata = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", moviePath);
            Process p = pb.start();
            p.waitFor();
            InputStream out = p.getErrorStream();
            InputStreamReader reader = new InputStreamReader(out);
            BufferedReader br = new BufferedReader(reader);
            String line;
            while( (line = br.readLine()) != null ) {
                if(line.contains("Metadata")) {
                    while( (line = br.readLine()) != null && (!line.contains("Duration")) ) {
                        String[] parts = line.split(":",2);
                        if(parts.length == 2)
                            metadata.put(parts[0].trim(), parts[1].trim());
                    }
                    // now get the duration
                    // Duration: 00:00:00.0, start=0000blahblah
                    String duration = line.substring(line.indexOf(":") + 1, line.indexOf(","));
                    metadata.put(TSDTVConstants.METADATA_DURATION_FIELD, duration);
                    break;
                }
            }

        } catch (InterruptedException | IOException e) {
            logger.error(e.getMessage());
        }

        return metadata;

    }

    private int getNumberOfEpisodes(String show) {
        int count = 0;
        File showDir = new File(catalogPath + "/" + show);
        if(showDir.exists()) {
            for(File f : showDir.listFiles()) {
                if(f.isFile()) count++;
            }
        } else {
            logger.error("Could not find show directory: {}", catalogPath + "/" + show);
        }

        return count;
    }

    private boolean hasSubtitles(String pathToMovie) {
        HashMap<Integer, StreamType> streams = getVideoStreams(pathToMovie);
        for(Integer streamNum : streams.keySet()) {
            if(streams.get(streamNum).equals(StreamType.SUBTITLES)) {
                return true;
            }
        }
        return false;
    }

    static class ThreadStream {
        public Thread thread;
        public TSDTVStream stream;

        public ThreadStream(Thread thread, TSDTVStream stream) {
            this.thread = thread;
            this.stream = stream;
        }

        public void begin() {
            thread.start();
        }

        public void kill() {
            thread.interrupt();
        }
    }

    public static class TSDTVBlock {
        public String id;
        public String name;
        public String[] scheduleParts;

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
