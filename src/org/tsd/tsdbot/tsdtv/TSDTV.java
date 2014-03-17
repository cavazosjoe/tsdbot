package org.tsd.tsdbot.tsdtv;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

/**
 * Created by Joe on 3/9/14.
 */
public class TSDTV {

    private static Logger logger = LoggerFactory.getLogger(TSDTV.class);

    private static final TSDTV instance = new TSDTV();

    private TSDBot bot = TSDBot.getInstance();
    private String scriptDir;
    private String catalogDir;
    private String scheduleLoc;

    private LinkedList<TSDTVProgram> queue = new LinkedList<>(); // file paths

    private Thread runningStream;

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

        bot.sendMessages(requester, IRCUtil.splitLongString(catalogBuilder.toString()));

    }

    private void play(TSDTVProgram program) {
        runningStream = new Thread(new TSDTVStream(scriptDir, program.filePath));
        runningStream.start();

        if(program.show != null && program.episodeNum > 0) {
            try {
                Connection dbConn = TSDDatabase.getInstance().getConnection();
                String update = "update TSDTV_SHOW set episodeNumber = ? where name = ?";
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

        bot.broadcast("[TSDTV] NOW PLAYING: " + metadata.get("Title") + " -- http://www.twitch.tv/tsd_irc");
    }

    public void prepareOnDemand(String dir, String query) throws Exception {

        if(runningStream != null)
            throw new Exception("There is already a stream running");

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

        play(new TSDTVProgram(matchedFiles.get(0).getAbsolutePath()));

    }

    public void prepareScheduledBlock(String blockName, LinkedList<String> programs) throws SQLException {

        logger.info("Preparing TSDTV block: {}", blockName);

        synchronized (this) {
            if(runningStream != null) {
                runningStream.notify(); // end running stream
                logger.info("Ended currently running stream");
            }
        }

        queue.clear();

        Connection dbConn = TSDDatabase.getInstance().getConnection();
        for(String show : programs) {
            // find the current episode to play and add it to the queue
            String q = String.format("select currentEpisode from TSDTV_SHOW where name = '%s'", show);
            try(PreparedStatement ps = dbConn.prepareStatement(q) ; ResultSet result = ps.executeQuery()) {
                while(result.next()) {
                    int episodeNum = result.getInt("episodeNumber");
                    logger.info("Looking for episode {} of {}", episodeNum, show);
                    File showDir = new File(catalogDir + "/" + show);
                    if(showDir.exists()) {
                        for(File f : showDir.listFiles()) {
                            if(f.getName().startsWith("" + episodeNum)) {
                                queue.addLast(new TSDTVProgram(f.getAbsolutePath(), show, episodeNum));
                                logger.info("Added {} to queue", f.getAbsolutePath());
                                break;
                            }
                        }
                    } else {
                        logger.error("Could not find show directory: {}", catalogDir + "/" + show);
                    }
                }
            }
        }

        StringBuilder broadcastBuilder = new StringBuilder();
        broadcastBuilder.append("[TSDTV] \"").append(blockName).append("\" block now starting. Lined up:");
        int i=0;
        while(i < Math.min(4, queue.size()+1)) {
            if(i != 0) broadcastBuilder.append(", ");
            broadcastBuilder.append(programs.get(i));
            i++;
        }

        bot.broadcast(broadcastBuilder.toString());

        play(queue.pop());
    }

    public void buildSchedule() {
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();

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
        } catch (Exception e) {
            logger.error("Error building TSDTV schedule", e);
        }
    }

    public void finishStream() {
        runningStream = null;
        if(!queue.isEmpty()) {
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
}
