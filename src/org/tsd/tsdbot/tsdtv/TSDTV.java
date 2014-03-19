package org.tsd.tsdbot.tsdtv;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.TSDBotLauncher;
import org.tsd.tsdbot.database.TSDDatabase;
import org.tsd.tsdbot.runnable.TSDTVStream;
import org.tsd.tsdbot.util.IRCUtil;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

/**
 * Created by Joe on 3/9/14.
 */
public class TSDTV {

    private static Logger logger = LoggerFactory.getLogger(TSDTV.class);

    private static final TSDTV instance = new TSDTV();

    private static final Pattern episodeNumberPattern = Pattern.compile("^(\\d+).*",Pattern.DOTALL);

    private String scriptDir;
    private String catalogDir;
    private String scheduleLoc;

    private Scheduler scheduler;
    private LinkedList<TSDTVProgram> queue = new LinkedList<>(); // file paths

    private ThreadStream runningStream;

    public TSDTV() {
        try {
            Properties prop = new Properties();
            InputStream fis = TSDBotLauncher.class.getResourceAsStream("/tsdbot.properties");
            prop.load(fis);
            catalogDir = prop.getProperty("tsdtv.catalog");
            scriptDir = prop.getProperty("mainDir");
            scheduleLoc = prop.getProperty("tsdtv.schedule");
        } catch (IOException e) {
            logger.error("Error initializing TSDTV", e);
        }
    }

    public static TSDTV getInstance() {
        return instance;
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
        TSDTVStream stream = new TSDTVStream(scriptDir, program.filePath);
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

        HashMap<String,String> metadata = getVideoMetadata(program.filePath);

        TSDBot.getInstance().broadcast("[TSDTV] NOW PLAYING: " + metadata.get("artist") + ": " +
                metadata.get("title") + " -- http://www.twitch.tv/tsd_irc");
    }

    public void prepareOnDemand(String channel, String dir, String query) throws Exception {

        File searchingDir;
        if(dir == null) searchingDir = new File(catalogDir);
        else {
            searchingDir = new File(catalogDir + "/" + dir);
            if(!searchingDir.exists())
                throw new Exception("Could not find directory " + dir + " (case sensitive)");
        }

        LinkedList<File> matchedFiles = new LinkedList<>();
        for(File f : searchingDir.listFiles()) {
            if(f.getName().toLowerCase().contains(query.toLowerCase()))
                matchedFiles.add(f);
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

        TSDTVProgram program = new TSDTVProgram(matchedFiles.get(0).getAbsolutePath());
        if(runningStream != null) {
            queue.addLast(program);
            TSDBot.getInstance().sendMessage(channel, "There is already a stream running. Your show has been enqueued");
        } else play(program);

    }

    public void prepareScheduledBlock(String blockName, LinkedList<String> programs) throws SQLException {

        logger.info("Preparing TSDTV block: {}", blockName);

        if(runningStream != null) {
            runningStream.kill(); // end running stream
            logger.info("Ended currently running stream");
        }

        queue.clear();

        Connection dbConn = TSDDatabase.getInstance().getConnection();

        // use dynamic map to get correct episode numbers for repeating shows
        HashMap<String, Integer> episodeNums = new HashMap<>(); // show -> episode num
        for(String show : programs) {

            int episodeNum = 0;
            if(!episodeNums.containsKey(show)) {
                // this show hasn't appeared in the block yet -- get current episode num from DB
                String q = String.format("select currentEpisode from TSDTV_SHOW where name = '%s'", show);
                try(PreparedStatement ps = dbConn.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
                    while(result.next()) {
                        episodeNum = result.getInt("currentEpisode");
                        episodeNums.put(show, episodeNum);
                    }
                }
            } else {
                // this show has appeared in the block -- increment episode num
                if(episodeNums.get(show)+1 > getNumberOfEpisodes(show)) episodeNum = 1; // wrap if we reached the end
                else episodeNum = episodeNums.get(show)+1;
                episodeNums.put(show,episodeNum);
            }

            logger.info("Looking for episode {} of {}", episodeNum, show);
            File showDir = new File(catalogDir + "/" + show);
            java.util.regex.Matcher epNumMatcher;
            if(showDir.exists()) {
                for(File f : showDir.listFiles()) {
                    epNumMatcher = episodeNumberPattern.matcher(f.getName());
                    while(epNumMatcher.find()) {
                        int epNum = Integer.parseInt(epNumMatcher.group(1));
                        if(epNum == episodeNum) {
                            queue.addLast(new TSDTVProgram(f.getAbsolutePath(), show, episodeNum));
                            logger.info("Added {} to queue", f.getAbsolutePath());
                            break;
                        }
                    }
                }
            } else {
                logger.error("Could not find show directory: {}", catalogDir + "/" + show);
            }
        }

        StringBuilder broadcastBuilder = new StringBuilder();
        broadcastBuilder.append("[TSDTV] \"").append(blockName).append("\" block now starting. Lined up: ");
        int i=0;
        while(i < Math.min(4, queue.size())) {
            if(i != 0) broadcastBuilder.append(", ");
            broadcastBuilder.append(programs.get(i));
            i++;
        }

        TSDBot.getInstance().broadcast(broadcastBuilder.toString());

        if(!queue.isEmpty()) play(queue.pop());
        else logger.error("Could not find any shows for block...");
    }

    public void printSchedule(String channel) {

        HashMap<String, String> metadata;

        if(runningStream != null) {
            metadata = getVideoMetadata(runningStream.stream.getMovie());
            String np = "NOW PLAYING: " + metadata.get("artist") + " - " + metadata.get("title");
            TSDBot.getInstance().sendMessage(channel, np);
        }

        if(!queue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("On deck: ");
            boolean first = true;
            for(TSDTVProgram program : queue) {
                if(!first) sb.append(", ");
                sb.append(program.show);
                first = false;
            }
            TSDBot.getInstance().sendMessage(channel, sb.toString());
        }

        try {
            Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.<JobKey>anyGroup());
            TreeMap<Date, JobDetail> jobMap = new TreeMap<>();
            for(JobKey key : keys) {
                List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(key);
                if(!triggers.isEmpty()) {
                    Date nextFireTime = triggers.get(0).getNextFireTime();
                    jobMap.put(nextFireTime, scheduler.getJobDetail(key));
                }
            }

            if(!jobMap.isEmpty()) {
                StringBuilder sb = null;
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm a z");
                sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                for(Date d : jobMap.descendingKeySet()) {
                    sb = new StringBuilder();
                    sb.append(sdf.format(d)).append(" -- ");
                    JobDetail job = jobMap.get(d);
                    String name = job.getJobDataMap().getString("name");
                    String schedule = job.getJobDataMap().getString("schedule");
                    sb.append(name).append(": ");
                    String[] scheduleParts = schedule.split(";;");
                    boolean first = true;
                    for(String s : scheduleParts) {
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
        }
    }

    public HashMap<String, String> getVideoMetadata(String moviePath) {

        HashMap<String, String> metadata = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", moviePath);
            pb.directory(new File(scriptDir));
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

    public int getNumberOfEpisodes(String show) {
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

    class ThreadStream {
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
}
