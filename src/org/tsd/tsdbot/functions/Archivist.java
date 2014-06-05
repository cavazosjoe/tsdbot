package org.tsd.tsdbot.functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Joe on 6/4/14.
 */
public class Archivist /*Exedol*/ implements MainFunction {

    private static final Logger log = LoggerFactory.getLogger(Archivist.class);

    private static final Archivist instance = null;

    public static final String stdPfx = "%s\t\t\t%d %s\t"; // MESSAGE 0000000   [00:00]
    public static final SimpleDateFormat stdSdf = new SimpleDateFormat("[MMM dd HH:mm]");

    private HashMap<String, FileWriter> writerMap = new HashMap<>();

    public Archivist(Properties properties, String[] channels) throws IOException {

        stdSdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

        String logDir = properties.getProperty("archiveDir");
        File logDirF = new File(logDir);
        if(!logDirF.exists()) {
            log.info("Logging directory " + logDir + " does not exist, creating...");
            if(logDirF.mkdir())
                log.info("Logging directory {} successfully created", logDir);
            else
                log.warn("Logging directory {} WAS NOT created", logDir);
        }

        for(String channel : channels) {
            log.info("Adding channel {} to Archivist", channel);
            File f = new File(logDir + "/" + channel.replace("#","") + ".log");
            if(!f.exists()) {
                log.info("Logging file {} does not exist, creating...", f.getAbsolutePath());
                if(f.createNewFile())
                    log.info("Logging file {} successfully created", f.getAbsolutePath());
                else
                    log.warn("Logging file {} WAS NOT created", f.getAbsolutePath());
            }

            if(!channel.startsWith("#")) channel = "#" + channel;
            writerMap.put(channel, new FileWriter(f, true));
        }
    }

    public void log(EventType eventType, String channel, Object... args) {
        String s = String.format(stdPfx + eventType.getFormat(), args);
        List<FileWriter> toWrite = new LinkedList<>();
        if(channel == null) {
            toWrite.addAll(writerMap.values());
        } else {
            FileWriter fw = writerMap.get(channel);
            if(fw != null)
                toWrite.add(fw);
            else
                log.warn("Could not find FileWriter for channel {}", channel);
        }

        for(FileWriter fw : toWrite) {
            try {
                fw.write(s + "\n");
                fw.flush();
            } catch (IOException e) {
                log.error("IOE while writing to FileWriter for " + channel, e);
            }
        }
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
    }

    public enum EventType {
        JOIN("* %s (%s@%s) has joined %s"),
        PART("* %s has quit (%s)"),
        MESSAGE("%s|%s\t\t\t%s"),
        CHANNEL_MODE("* %s sets mode %s for %s"),
        USER_MODE("* %s sets mode %s for %s"),
        TOPIC("* %s has changed the topic to \"%s\""),
        ACTION("* %s %s"),
        NICK_CHANGE("* %s is now known as %s"),
        KICK("* %s has kicked %s from %s (%s)");

        private String format;

        EventType(String format) {
            this.format = format;
        }

        public String getFormat() {
            return format;
        }
    }
}
