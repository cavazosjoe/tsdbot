package org.tsd.tsdbot.functions;

import org.apache.commons.lang3.StringUtils;
import org.jibble.pircbot.User;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.database.TSDDatabase;
import org.tsd.tsdbot.runnable.TSDTVStream;
import org.tsd.tsdbot.scheduled.SchedulerConstants;
import org.tsd.tsdbot.tsdtv.TSDTVBlockJob;
import org.tsd.tsdbot.tsdtv.TSDTVConstants;
import org.tsd.tsdbot.tsdtv.TSDTVProgram;
import org.tsd.tsdbot.util.FuzzyLogic;
import org.tsd.tsdbot.util.IRCUtil;

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
public class TSDTV extends MainFunction {

    private static Logger logger = LoggerFactory.getLogger(TSDTV.class);

    private static final TSDTV instance = new TSDTV();

    private static final Pattern episodeNumberPattern = Pattern.compile("^(\\d+).*",Pattern.DOTALL);
    private static final int dayBoundaryHour = 4; // 4:00 AM
    private static final TimeZone timeZone = TimeZone.getTimeZone("America/New_York");

    private String catalogDir;
    private String scheduleLoc;
    private String ffmpegExec;

    private LinkedList<TSDTVProgram> queue = new LinkedList<>(); // file paths

    private ThreadStream runningStream;

    private TSDTV() {
        Properties prop = new Properties();
        try(InputStream fis = TSDTV.class.getResourceAsStream("/tsdbot.properties")) {
            prop.load(fis);
            catalogDir = prop.getProperty("tsdtv.catalog");
            scheduleLoc = prop.getProperty("tsdtv.schedule");
            ffmpegExec = prop.getProperty("tsdtv.ffmpeg");
        } catch (IOException e) {
            logger.error("Error initializing TSDTV", e);
        }
    }

    public static TSDTV getInstance() {
        return instance;
    }
    
    @Override
    public void run(String channel, String sender, String ident, String text) {

        TSDBot bot = TSDBot.getInstance();
        String[] cmdParts = text.split("\\s+");
        TSDBot.Command cmd = TSDBot.Command.TSDTV;
        
        if(cmdParts.length < 2) {
            bot.sendMessage(channel, cmd.getUsage());
            return;
        }

        String subCmd = cmdParts[1];

        if(subCmd.equals("catalog")) {

            String subdir = null;
            if(cmdParts.length > 2) {
                subdir = cmdParts[2].replaceAll("/","");
            }

            try {
                bot.sendMessage(channel, "I'm sending you a list of my available movies, " + sender);
                catalog(sender, subdir);
            } catch (Exception e) {
                bot.sendMessage(channel, "Error retrieving catalog: " + e.getMessage());
            }

        } else if(subCmd.equals("replay")) {

            if(cmdParts.length < 3) {
                bot.sendMessage(channel, cmd.getUsage());
                return;
            }

            prepareBlockReplay(channel, cmdParts[2]);

        } else if(subCmd.equals("play")) {

            String subdir = null;
            String query;
            if(cmdParts.length > 3) {
                subdir = cmdParts[2].replaceAll("/","");
                query = cmdParts[3].replaceAll("/", "");
            } else {
                query = cmdParts[2].replaceAll("/","");
            }

            try {
                prepareOnDemand(channel, subdir, query);
            } catch (Exception e) {
                bot.sendMessage(channel, "Error: " + e.getMessage());
            }

        } else if(subCmd.equals("kill")) {

            if(!bot.getUserFromNick(channel, sender).hasPriv(User.Priv.OP)) {
                bot.sendMessage(channel, "Only ops can use that");
                return;
            }
            kill();
            bot.sendMessage(channel, "The stream has been killed");

        } else if(subCmd.equals("reload")) {

            if(!bot.getUserFromNick(channel, sender).hasPriv(User.Priv.OP)) {
                bot.sendMessage(channel, "Only ops can use that");
                return;
            }
            buildSchedule(TSDBot.getInstance().getScheduler());
            bot.sendMessage(channel, "The schedule has been reloaded");

        } else if(subCmd.equals("schedule")) {

            if(cmdParts.length > 2 && cmdParts[2].equalsIgnoreCase("all"))
                printSchedule(channel, false);
            else
                printSchedule(channel, true);

        } else if(subCmd.equals("viewers")) {

            int count = getViewerCount();
            String msg;
            switch (count) {
                case -1: msg = "An error occurred getting the viewer count"; break;
                case 1: msg = "There is 1 viewer watching the stream"; break;
                default: msg = "There are " + count + " viewers watching the stream"; break;
            }
            if(runningStream == null) {
                msg += ". But there isn't a stream running";
            }
            bot.sendMessage(channel, msg);

        } else if(subCmd.equals("current")) {

            // .tsdtv current ippo
            if(cmdParts.length > 2) {
                try {
                    File show = getFuzzyShow(cmdParts[2]);
                    int nextEpisode = getCurrentEpisode(show.getName());
                    if(nextEpisode > getNumberOfEpisodes(show.getName()))
                        nextEpisode = 1;

                    int prevEpisode = getCurrentEpisode(show.getName()) - 1;
                    if(prevEpisode < 1)
                        prevEpisode = getNumberOfEpisodes(show.getName());

                    StringBuilder sb = new StringBuilder();
                    sb.append("The next episode of ")
                            .append(show.getName())
                            .append(" will be ")
                            .append(nextEpisode)
                            .append(". The previously watched episode was ")
                            .append(prevEpisode)
                            .append(".");
                    bot.sendMessage(channel, sb.toString());
                } catch (Exception e) {
                    bot.sendMessage(channel, "Error: " + e.getMessage());
                }
            } else {
                bot.sendMessage(channel, cmd.getUsage());
            }
        } else if(subCmd.equals("links")) {
            bot.sendMessage(channel, getLinks(true));
        }
    }

    public void catalog(String requester, String subdir) throws Exception {
        File printingDir;
        if(subdir == null)
            printingDir = new File(catalogDir);
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

        TSDBot.getInstance().sendMessages(requester, IRCUtil.splitLongString(catalogBuilder.toString()));

    }

    private void play(TSDTVProgram program) {
        TSDTVStream stream = new TSDTVStream(ffmpegCommand(program.filePath), program.filePath);
        Thread thread = new Thread(stream);
        runningStream = new ThreadStream(thread, stream);
        runningStream.begin();

        if(program.show != null && program.episodeNum > 0) {
            try {
                Connection dbConn = TSDDatabase.getInstance().getConnection();
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
            TSDBot.getInstance().broadcast(msg.replaceAll("_"," "));
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
        if(runningStream != null) {
            queue.addLast(program);
            TSDBot.getInstance().sendMessage(channel, "There is already a stream running. Your show has been enqueued");
        } else play(program);

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

                File showDir = new File(catalogDir + "/" + show);
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

        TSDBot.getInstance().broadcast(broadcastBuilder.toString());

        if(blockIntro != null) {
            // there's a block intro playing, link people to the stream while it plays
            TSDBot.getInstance().broadcast(getLinks(false));
        }

        if(!queue.isEmpty()) play(queue.pop());
        else logger.error("Could not find any shows for block...");
    }

    public void prepareBlockReplay(String channel, String blockQuery) {

        logger.info("Preparing TSDTV block rerun: {}", blockQuery);

        if(runningStream != null) {
            TSDBot.getInstance().sendMessage(channel, "There is already a stream running, please wait for it to" +
                    " finish  before starting a block rerun");
            return;
        }

        try {
            final Scheduler scheduler = TSDBot.getInstance().getScheduler();
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
                TSDBot.getInstance().sendMessage(channel, "Could not find any blocks matching " + blockQuery);
            } else if(matchedJobs.size() > 1) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for(JobKey jobKey : matchedJobs) {
                    JobDetail job = scheduler.getJobDetail(jobKey);
                    if(!first) sb.append(", ");
                    sb.append(job.getJobDataMap().getString(SchedulerConstants.TSDTV_BLOCK_NAME_FIELD));
                    first = false;
                }
                TSDBot.getInstance().sendMessage(channel, "Found multiple blocks matching \"" + blockQuery + "\": " + sb.toString());
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
            TSDBot.getInstance().sendMessage(channel, "(Error retrieving scheduled info)");
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

            TSDBot.getInstance().sendMessage(channel, "NOW PLAYING: " + np);
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
            TSDBot.getInstance().sendMessage(channel, sb.toString());
        }

        try {

            GregorianCalendar endOfToday = new GregorianCalendar();
            endOfToday.setTimeZone(timeZone);
            endOfToday.set(Calendar.HOUR_OF_DAY, dayBoundaryHour);
            if(endOfToday.get(Calendar.HOUR_OF_DAY) >= dayBoundaryHour)
                endOfToday.add(Calendar.DATE, 1);

            Scheduler scheduler = TSDBot.getInstance().getScheduler();
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
                    TSDBot.getInstance().sendMessage(channel, sb.toString());
                }
            }

        } catch (SchedulerException e) {
            TSDBot.getInstance().sendMessage(channel, "(Error retrieving scheduled info)");
            logger.error("Error getting scheduled info", e);
        }
    }

    public void buildSchedule(Scheduler scheduler) {
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

    private File getFuzzyShow(String query) throws Exception {

        // return the File object, use it to getName or getPath
        List<File> matchingDirs = FuzzyLogic.fuzzySubset(
                query,
                Arrays.asList(new File(catalogDir).listFiles()),
                new FuzzyLogic.FuzzyVisitor<File>() {
                    @Override
                    public String visit(File o1) {
                        return o1.getName();
                    }
        });

        if(matchingDirs.size() == 0)
            throw new Exception("Could not find directory matching \"" + query + "\"");
        else if(matchingDirs.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("Found multiple directories matching for \"").append(query).append("\":");
            for(File f : matchingDirs)
                sb.append(" ").append(f.getName());
            throw new Exception(sb.toString());
        } else {
            return matchingDirs.get(0);
        }
    }

    private String getEpisode(String show, int episodeNumber) {
        File showDir = new File(catalogDir + "/" + show);
        java.util.regex.Matcher epNumMatcher;
        if(showDir.exists()) {
            for(File f : showDir.listFiles()) {
                epNumMatcher = episodeNumberPattern.matcher(f.getName());
                while(epNumMatcher.find()) {
                    int epNum = Integer.parseInt(epNumMatcher.group(1));
                    if(epNum == episodeNumber) {
                        return f.getAbsolutePath();
                    }
                }
            }
        } else {
            logger.error("Could not find show directory: {}", catalogDir + "/" + show);
        }

        return null;
    }

    private String getShowIntro(String show) {
        File introDir = new File(catalogDir + "/" + show + "/" + TSDTVConstants.INTRO_DIR_NAME);
        return getRandomFilePathFromDirectory(introDir);
    }

    private String getBlockIntro(String blockId) {
        File introDir = new File(catalogDir + "/"
                + TSDTVConstants.BLOCKS_DIR_NAME + "/" + blockId + "/" + TSDTVConstants.INTRO_DIR_NAME);
        return getRandomFilePathFromDirectory(introDir);
    }

    private File getRandomFileFromDirectory(File dir) {
        if(dir.exists()) {
            File[] files = dir.listFiles();
            if(files == null || files.length == 0)
                return null;
            Random rand = new Random();
            return files[rand.nextInt(files.length)];
        }
        return null;
    }

    private String getRandomFilePathFromDirectory(File dir) {
        File f = getRandomFileFromDirectory(dir);
        if(f == null) return null;
        else return f.getAbsolutePath();
    }

    private int getCurrentEpisode(String show) throws SQLException {
        Connection dbConn = TSDDatabase.getInstance().getConnection();
        String q = String.format("select currentEpisode from TSDTV_SHOW where name = '%s'", show);
        try(PreparedStatement ps = dbConn.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            if(result.next()) {
                return result.getInt("currentEpisode");
            }
        }
        return -1;
    }

    private int getViewerCount() {

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

    private String getLinks(boolean includeVlc) {
        StringBuilder sb = new StringBuilder();
        sb.append("PREMIUM: http://irc.teamschoolyd.org/tsdtv.html -- POVERTY: http://irc.teamschoolyd.org/tsdtv-poverty.html");
        if(includeVlc)
            sb.append(" -- VLC: http://irc.teamschoolyd.org:8090/premium.flv | http://irc.teamschoolyd.org:8090/poverty.flv");
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
        File showDir = new File(catalogDir + "/" + show);
        if(showDir.exists()) {
            for(File f : showDir.listFiles()) {
                if(f.isFile()) count++;
            }
        } else {
            logger.error("Could not find show directory: {}", catalogDir + "/" + show);
        }

        return count;
    }

    private String[] ffmpegCommand(String targetFile) {
        return new String[]{
                "nice",     "-n","8",
                ffmpegExec,
                "-re",
                "-y",
                "-i",       targetFile,
                "http://localhost:8090/feed1.ffm"
        };
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
