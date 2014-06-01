package org.tsd.tsdbot.functions;

import org.jibble.pircbot.User;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.TSDBotLauncher;
import org.tsd.tsdbot.database.TSDDatabase;
import org.tsd.tsdbot.runnable.TSDTVStream;
import org.tsd.tsdbot.tsdtv.TSDTVBlock;
import org.tsd.tsdbot.tsdtv.TSDTVProgram;
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
public class TSDTV implements MainFunction {

    private static Logger logger = LoggerFactory.getLogger(TSDTV.class);

    private static final TSDTV instance = new TSDTV();

    private static final Pattern episodeNumberPattern = Pattern.compile("^(\\d+).*",Pattern.DOTALL);
    private static final int DAY_BOUNDARY_HOUR = 4; // 4:00 AM
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("America/New_York");

    private String catalogDir;
    private String scheduleLoc;
    private String ffmpegExec;

    private Scheduler scheduler;
    private LinkedList<TSDTVProgram> queue = new LinkedList<>(); // file paths

    private ThreadStream runningStream;

    private TSDTV() {
        try {
            Properties prop = new Properties();
            InputStream fis = TSDTV.class.getResourceAsStream("/tsdbot.properties");
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
    public void run(String channel, String sender, String text) {

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
            buildSchedule();
            bot.sendMessage(channel, "The schedule has been reloaded");

        } else if(subCmd.equals("schedule")) {

            if(cmdParts.length > 2 && cmdParts[2].equalsIgnoreCase("all"))
                printSchedule(channel, false);
            else
                printSchedule(channel, true);

        }
    }

    public void catalog(String requester, String subdir) throws Exception {
        File printingDir;
        if(subdir == null) printingDir = new File(catalogDir);
        else {
            printingDir = new File(catalogDir + "/" + subdir);
            if(!printingDir.exists())
                throw new Exception("Could not locate directory " + subdir + " (case sensitive)");
        }

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
                    ps.setInt(1,program.episodeNum+1);
                    ps.setString(2,program.show);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                logger.error("Error updating show episode number", e);
            }
        }

        if(!program.show.startsWith(".")) { // skip commercials and bumps
            HashMap<String,String> metadata = getVideoMetadata(program.filePath);
            String artist = metadata.get("artist");
            if(artist == null) artist = program.show;
            String title = metadata.get("title");
            if(title == null) title = program.filePath.substring(program.filePath.lastIndexOf("/")+1);

            String msg = "[TSDTV] NOW PLAYING: " + artist + ": " + title +
                    " -- PREMIUM: http://irc.teamschoolyd.org/tsdtv.html" +
                    " -- POVERTY: http://irc.teamschoolyd.org/tsdtv-poverty.html" +
                    " -- VLC: http://irc.teamschoolyd.org:8090/premium.flv | http://irc.teamschoolyd.org:8090/poverty.flv";
            TSDBot.getInstance().broadcast(msg.replaceAll("_"," "));
        }
    }

    public void prepareOnDemand(String channel, String dir, String query) throws Exception {

        File searchingDir;
        String show = null;
        if(dir == null) searchingDir = new File(catalogDir);
        else {
            List<File> matchingDirs = new LinkedList<>();
            for(File f : (new File(catalogDir)).listFiles()) {
                if(f.isDirectory() && f.getName().toLowerCase().contains(dir.toLowerCase()))
                    matchingDirs.add(f);
            }

            if(matchingDirs.size() == 0)
                throw new Exception("Could not find directory matching \"" + dir + "\"");
            else if(matchingDirs.size() > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("Found multiple directories matching for \"").append(dir).append("\":");
                for(File f : matchingDirs)
                    sb.append(" ").append(f.getName());
                throw new Exception(sb.toString());
            } else {
                searchingDir = matchingDirs.get(0);
                show = searchingDir.getName();
            }
        }

        LinkedList<File> matchedFiles = new LinkedList<>();
        if("random".equals(query)) {

            Random rand = new Random();
            int size = searchingDir.listFiles().length;
            matchedFiles.add(searchingDir.listFiles()[rand.nextInt(size)]);

        } else {

            for(File f : searchingDir.listFiles()) {
                if(f.getName().toLowerCase().contains(query.toLowerCase()))
                    matchedFiles.add(f);
            }

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

    public void prepareScheduledBlock(String blockName, LinkedList<String> programs, int offset) throws SQLException {

        logger.info("Preparing TSDTV block: {} with offset {}", blockName, offset);

        if(runningStream != null) {
            runningStream.kill(); // end running stream
            logger.info("Ended currently running stream");
        }

        queue.clear();

        // use dynamic map to get correct episode numbers for repeating shows
        // use offset to handle replays/reruns
        HashMap<String, Integer> episodeNums = new HashMap<>(); // show -> episode num
        for(String show : programs) {

            if(show.startsWith(".")) { // commercial or bump, grab random

                Random rand = new Random();
                File showDir = new File(catalogDir + "/" + show);
                if(showDir.exists()) {
                    List<File> files = Arrays.asList(showDir.listFiles());
                    File f = files.get(rand.nextInt(files.size()));
                    queue.addLast(new TSDTVProgram(f.getAbsolutePath(), show));
                    logger.info("Added {} to queue", f.getAbsolutePath());
                } else {
                    logger.error("Could not find show directory: {}", catalogDir + "/" + show);
                }

            } else {

                int episodeNum = 0;
                if(!episodeNums.containsKey(show)) {
                    // this show hasn't appeared in the block yet -- get current episode num from DB
                    episodeNum = getCurrentEpisode(show) + offset;
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
        broadcastBuilder.append("[TSDTV] \"").append(blockName).append("\" block now starting. Lined up: ");

        String lastProgram = "";
        boolean first = true;
        for(String program : programs) {
            if(program.startsWith(".")) continue;
            if(!lastProgram.equals(program)) {
                if(!first) broadcastBuilder.append(", ");
                broadcastBuilder.append(program);
                lastProgram = program;
                first = false;
            }
        }

        TSDBot.getInstance().broadcast(broadcastBuilder.toString());

        if(!queue.isEmpty()) play(queue.pop());
        else logger.error("Could not find any shows for block...");
    }

    public void prepareBlockReplay(String channel, String block) {

        logger.info("Preparing TSDTV block rerun: {}", block);

        if(runningStream != null) {
            TSDBot.getInstance().sendMessage(channel, "There is already a stream running, please wait for it to" +
                    " finish  before starting a block rerun");
            return;
        }

        try {
            Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.<JobKey>anyGroup());
            LinkedList<JobDetail> matchedJobs = new LinkedList<>();
            for(JobKey key : keys) {
                JobDetail jobDetail = scheduler.getJobDetail(key);
                String name = jobDetail.getJobDataMap().getString("name");
                if(IRCUtil.fuzzyMatches(block, name))
                    matchedJobs.add(jobDetail);
            }

            if(matchedJobs.size() == 0) {
                TSDBot.getInstance().sendMessage(channel, "Could not find any blocks matching " + block);
            } else if(matchedJobs.size() > 1) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for(JobDetail job : matchedJobs) {
                    if(!first) sb.append(", ");
                    sb.append(job.getJobDataMap().getString("name"));
                    first = false;
                }
                TSDBot.getInstance().sendMessage(channel, "Found multiple blocks matching \"" + block + "\": " + sb.toString());
            } else {
                JobDetail job = matchedJobs.get(0);
                String name = job.getJobDataMap().getString("name");
                String schedule = job.getJobDataMap().getString("schedule");
                String[] scheduleParts = schedule.split(";;");

                LinkedList<String> blockSchedule = new LinkedList<>();
                Collections.addAll(blockSchedule, scheduleParts);

                try {
                    prepareScheduledBlock(name, blockSchedule, -1);
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
            String artist = metadata.get("artist");
            String title = metadata.get("title");

            String np;
            if(artist == null || title == null) np = runningStream.stream.getMovieName();
            else np = metadata.get("artist") + " - " + metadata.get("title");

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
            endOfToday.setTimeZone(TIME_ZONE);
            endOfToday.set(Calendar.HOUR_OF_DAY, DAY_BOUNDARY_HOUR);
            if(endOfToday.get(Calendar.HOUR_OF_DAY) >= DAY_BOUNDARY_HOUR)
                endOfToday.add(Calendar.DATE, 1);

            Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.<JobKey>anyGroup());
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
                sdf.setTimeZone(TIME_ZONE);
                for(Date d : jobMap.keySet()) {
                    sb = new StringBuilder();
                    sb.append(sdf.format(d)).append(" -- ");
                    JobDetail job = jobMap.get(d);
                    String name = job.getJobDataMap().getString("name");
                    String schedule = job.getJobDataMap().getString("schedule");
                    sb.append(name).append(": ");
                    String[] scheduleParts = schedule.split(";;");
                    boolean first = true;
                    for(String s : scheduleParts) {
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

    public void buildSchedule() {
        try {
            if(scheduler == null) {
                SchedulerFactory schedulerFactory = new StdSchedulerFactory();
                scheduler = schedulerFactory.getScheduler();
            } else {
                scheduler.clear();
            }

            JobDetail job;
            CronTrigger cronTrigger;

            FileInputStream schedule = new FileInputStream(new File(scheduleLoc));
            try(BufferedReader br = new BufferedReader(new InputStreamReader(schedule))) {
                String line = null;
                while((line = br.readLine()) != null) {
                    if(line.startsWith("BLOCK")) {
                        String blockName = line.substring(line.indexOf("=") + 1);
                        String quartzString = br.readLine();
                        LinkedList<String> shows = new LinkedList<>();
                        while(!(line = br.readLine()).equals("ENDBLOCK")) {
                            shows.add(line);
                        }

                        StringBuilder scheduleBuilder = new StringBuilder();
                        boolean first = true;
                        for(String show : shows) {
                            if(!first) scheduleBuilder.append(";;");
                            scheduleBuilder.append(show);
                            first = false;
                        }

                        job = newJob(TSDTVBlock.class)
                                .withIdentity(blockName)
                                .usingJobData("schedule", scheduleBuilder.toString())
                                .usingJobData("name", blockName)
                                .build();

                        cronTrigger = newTrigger()
                                .withSchedule(cronSchedule(quartzString))
                                .build();

                        scheduler.scheduleJob(job, cronTrigger);
                    }
                }
            }

            scheduler.start();

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

    private int getCurrentEpisode(String show) throws SQLException {
        Connection dbConn = TSDDatabase.getInstance().getConnection();
        String q = String.format("select currentEpisode from TSDTV_SHOW where name = '%s'", show);
        try(PreparedStatement ps = dbConn.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
            while(result.next()) {
                return result.getInt("currentEpisode");
            }
        }
        return -1;
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
                    metadata.put("duration",duration);
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
