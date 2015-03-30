package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.AllChannels;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.Function;
import org.tsd.tsdbot.util.ArchivistUtil;
import org.tsd.tsdbot.util.IRCUtil;
import org.tsd.tsdbot.util.MiscUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Joe on 6/4/14.
 */
@Singleton
@Function(initialRegex = "^\\.catchup.*")
public class Archivist /*Exedol*/ extends MainFunctionImpl {

    private static final Logger log = LoggerFactory.getLogger(Archivist.class);

    private static final int timestampColumnWidth = 15; // [JUN 10 07:47] + space
    private static final int middleColumnWidth = 31; // max nick length = 30 + space
    private static final String prettyPrefixFormat = "%-"+timestampColumnWidth+"s%"+middleColumnWidth+"s||";

    public static final SimpleDateFormat stdSdf = new SimpleDateFormat("[MMM dd HH:mm]");

    private static final long fiveMinutes = 1000 * 60 * 5;

    private static String archiveDir = null;
    private static String recapDir = null;

    private HashMap<String, PrintWriter> writerMap = new HashMap<>();

    @Inject
    public Archivist(Bot bot, Properties properties, @AllChannels List channels) throws IOException {

        super(bot);

        this.description = "Catchup function. Get a personalized review of what you missed";
        this.usage = ".catchup [ minutes (integer) ]";

        stdSdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

        recapDir = properties.getProperty("archivist.recaps");
        archiveDir = properties.getProperty("archivist.logs");
        File logDirF = new File(archiveDir);
        if(!logDirF.exists()) {
            log.info("Logging directory {} does not exist, creating...", archiveDir);
            if(logDirF.mkdir())
                log.info("Logging directory {} successfully created", archiveDir);
            else
                log.warn("Logging directory {} WAS NOT created", archiveDir);
        }

        // use injected list of channels so this can be initialized before the bot joins any
        for(Object o : channels) {
            String channel = (String) o;
            log.info("Adding channel {} to Archivist", channel);
            File f = new File(archiveDir + channel.replace("#","") + ".log");
            if(!f.exists()) {
                log.info("Logging file {} does not exist, creating...", f.getAbsolutePath());
                if(f.createNewFile())
                    log.info("Logging file {} successfully created", f.getAbsolutePath());
                else
                    log.warn("Logging file {} WAS NOT created", f.getAbsolutePath());
            }

            if(!channel.startsWith("#"))
                channel = "#" + channel;
            writerMap.put(channel, new PrintWriter(new FileWriter(f, true)));
        }
    }

    public void log(String channel, String record) {

        List<PrintWriter> toWrite = new LinkedList<>();
        if(channel == null) {
            toWrite.addAll(writerMap.values());
        } else {
            PrintWriter pw = writerMap.get(channel);
            if(pw != null)
                toWrite.add(pw);
            else
                log.warn("Could not find FileWriter for channel {}", channel);
        }

        for(PrintWriter pw : toWrite) {
            pw.println(record);
            pw.flush();
        }
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        String[] cmdParts = text.split("\\s+");

        if(cmdParts.length == 1) {

            String niceChannel = channel.replace("#","");
            try(BufferedReader br = new BufferedReader(new FileReader(archiveDir + niceChannel + ".log"))) {

                LinkedList<String> capturedText = new LinkedList<>();
                LinkedList<String> captureBuffer = new LinkedList<>();
                boolean recording = false;

                // value must be list because multiple events can happen on same millisecond
                TreeMap<Long, LinkedList<String>> pastFiveMinutes = new TreeMap<>(); // timestamp -> message

                String line;
                while( (line = br.readLine()) != null ) {

                    EventType lineEvent = EventType.getFromRaw(line);

                    if(lineEvent.equals(EventType.JOIN)) {
                        // someone joined, analyze to see if it's our guy
                        String[] parts = line.split("\\s");
                        if(ident.equals(parts[3])) {
                            // it's a match, save buffer if it's not empty, otherwise save past five minutes
                            if(captureBuffer.isEmpty()) {
                                capturedText = new LinkedList<>();
                                capturedText.add("--- YOU JOINED WITHOUT LEAVING FIRST: DISPLAYING FIVE MINUTES OF " +
                                        "CHAT PRIOR TO YOUR LAST JOIN ---");
                                for(Long key : pastFiveMinutes.keySet()) {
                                    capturedText.addAll(pastFiveMinutes.get(key));
                                }
                            } else {
                                captureBuffer.addLast(line);
                                capturedText = (LinkedList<String>) captureBuffer.clone();
                            }
                            captureBuffer.clear(); // clear the buffer when we detect the person joined
                            recording = false;
                        }
                    } else if(lineEvent.equals(EventType.PART) || lineEvent.equals(EventType.QUIT)) {
                        // someone left, analyze to see if it's our guy
                        String[] parts = line.split("\\s");
                        if(ident.equals(parts[3])) {
                            // it's a match, if we're not recording, start
                            // if we are recording, dump what's been captured -- the guy was here to see it anyway
                            if(recording) {
                                captureBuffer.clear();
                                capturedText.clear();
                            }
                            recording = true;
                        }
                    }

                    if(recording) {
                        captureBuffer.addLast(line);
                    }

                    // get the message's time
                    Long now = Long.parseLong(line.split("\\s")[1]);
                    if(pastFiveMinutes.containsKey(now)) {
                        pastFiveMinutes.get(now).addLast(lineEvent.getPrettyFormatted(line));
                    } else {
                        LinkedList<String> m = new LinkedList<>();
                        m.add(lineEvent.getPrettyFormatted(line));
                        pastFiveMinutes.put(now, m);
                    }

                    pastFiveMinutes = trimFiveMinuteBuffer(pastFiveMinutes, now);

                }

                StringBuilder output = new StringBuilder();

                if(capturedText.isEmpty()) {
                    for(Long key : pastFiveMinutes.keySet()) {
                        for(String s : pastFiveMinutes.get(key)) {
                            log.info("-5MIN RECAP || " + s);
                            output.append(EventType.getFromRaw(s).getPrettyFormatted(s)).append(IRCUtil.LINE_SEPARATOR);
                        }
                    }
                } else {
                    for(String s : capturedText) {
                        log.info("RECAP || " + s);
                        output.append(EventType.getFromRaw(s).getPrettyFormatted(s)).append(IRCUtil.LINE_SEPARATOR);
                    }
                }

                if(!(output.toString().isEmpty())) {
                    if(capturedText.isEmpty())
                        bot.sendMessage(channel, "I don't think you've missed anything. I'll recap the last five minutes");

                    String fileName = MiscUtils.getRandomString() + ".txt";
                    File recapFile = new File(recapDir + fileName);
                    try(BufferedWriter writer = new BufferedWriter(new FileWriter(recapFile))) {
                        writer.write(output.toString());
                        writer.flush();
                        bot.sendMessage(channel, "http://irc.teamschoolyd.org/recaps/" + fileName);
                    }
                }

            } catch (Exception e) {
                log.error("Error reading logging file",e);
            }

        } else { // .recap 15
            Integer minutes;
            try {
                minutes = Integer.parseInt(cmdParts[1]);

                String niceChannel = channel.replace("#","");
                try(BufferedReader br = new BufferedReader(new FileReader(archiveDir + niceChannel + ".log"))) {
                    String line;
                    boolean recording = false;
                    Long now = System.currentTimeMillis();
                    Long timestamp;
                    LinkedList<String> catchup = new LinkedList<>();
                    while( (line = br.readLine()) != null ) {
                        timestamp = Long.parseLong(line.split("\\s")[1]);
                        if(recording || now - timestamp < 1000 * 60 * minutes) {
                            catchup.addLast(line);
                            recording = true;
                        }
                    }

                    StringBuilder output = new StringBuilder();
                    for(String s : catchup) {
                        output.append(EventType.getFromRaw(s).getPrettyFormatted(s)).append(IRCUtil.LINE_SEPARATOR);
                    }

                    String fileName = MiscUtils.getRandomString() + ".txt";
                    File recapFile = new File(recapDir + fileName);
                    try(BufferedWriter writer = new BufferedWriter(new FileWriter(recapFile))) {
                        writer.write(output.toString());
                        writer.flush();
                        bot.sendMessage(channel, "Here are the chat logs from the past " + minutes + " minutes: http://irc.teamschoolyd.org/recaps/" + fileName);
                    }

                } catch (Exception e) {
                    log.error("There was an error processing the channel archive", e);
                }

            } catch (NumberFormatException nfe) {
                // the second argument isn't a number
                bot.sendMessage(channel, usage);
            }
        }
    }

    private TreeMap<Long, LinkedList<String>> trimFiveMinuteBuffer(TreeMap<Long, LinkedList<String>> buffer, long fromTime) {
        Iterator it = buffer.keySet().iterator();
        while(it.hasNext()) {
            Long timestamp = (Long) it.next();
            if(fromTime - timestamp < fiveMinutes) break; // we've reached something less than five minutes old, stop trimming
            it.remove();
        }
        return buffer;
    }

    public static enum EventType {
        JOIN("%s (%s@%s) has joined %s") {
            @Override
            public String getPrettyFormatted(String raw) {
                // 0:JOIN 1:99999999 2:nickname 3:ident 4:host 5:channel
                // *||Schooly_D (schoolyd@tsd.org) has joined #tsd
                String[] parts = raw.split("\\s");
                return String.format(prettyFormat, stdSdf.format(new Date(Long.parseLong(parts[1]))), "*", parts[2], parts[3], parts[4], parts[5]);
            }
        },
        PART("%s (%s) has left %s") {
            @Override
            public String getPrettyFormatted(String raw) {
                // 0:PART 1:99999999 2:nickname 3:ident 4:channel
                // *||Schooly_D (schoolyd) has left #tsd
                String[] parts = raw.split("\\s");
                return String.format(prettyFormat, stdSdf.format(new Date(Long.parseLong(parts[1]))), "*", parts[2], parts[3], parts[4]);
            }
        },
        QUIT("%s (%s) has quit (%s)") {
            public void blah() {

            }
            @Override
            public String getPrettyFormatted(String raw) {
                // 0:PART 1:99999999 2:nickname 3:ident 4-N:reason
                // *||Schooly_D (schoolyd) has quit (Quit: Bye bye bye)
                String[] parts = raw.split("\\s");
                String reason = ArchivistUtil.compileMessage(parts, 4);
                return String.format(prettyFormat, stdSdf.format(new Date(Long.parseLong(parts[1]))), "*", parts[2], parts[3], reason);
            }

        },
        MESSAGE("%s") {
            @Override
            public String getPrettyFormatted(String raw) {
                // 0:MESSAGE 1:9999999 2:nickname 3:ident 4-N:message
                // Schooly_D||Hey hey hey what's going on?
                String[] parts = raw.split("\\s");
                String message = ArchivistUtil.compileMessage(parts, 4);
                return String.format(prettyFormat, stdSdf.format(new Date(Long.parseLong(parts[1]))), parts[2], message);
            }
        },
        CHANNEL_MODE("%s sets mode %s for %s") {
            @Override
            public String getPrettyFormatted(String raw) {
                // 0:CHANNEL_MODE 1:9999999 2:nickname 3:mode 4:channel
                // *||Schooly_D sets mode +m for #tsd
                String[] parts = raw.split("\\s");
                return String.format(prettyFormat, stdSdf.format(new Date(Long.parseLong(parts[1]))), "*", parts[2], parts[3], parts[4]);
            }
        },
        USER_MODE("%s sets mode %s for %s") {
            @Override
            public String getPrettyFormatted(String raw) {
                // 0:USER_MODE 1:9999999 2:nickname 3:mode 4:user
                // *||Schooly_D sets mode +b for NartFOpc
                String[] parts = raw.split("\\s");
                return String.format(prettyFormat, stdSdf.format(new Date(Long.parseLong(parts[1]))), "*", parts[2], parts[3], parts[4]);
            }
        },
        TOPIC("%s has changed the topic to \"%s\"") {
            @Override
            public String getPrettyFormatted(String raw) {
                // 0:TOPIC 1:9999999 2:nickname 3-N:topic
                // *||Schooly_D has changed the topic to "blah blah blah"
                String[] parts = raw.split("\\s");
                String topic = ArchivistUtil.compileMessage(parts, 3);
                return String.format(prettyFormat, stdSdf.format(new Date(Long.parseLong(parts[1]))), "*", parts[2], topic);
            }
        },
        ACTION("%s %s") {
            @Override
            public String getPrettyFormatted(String raw) {
                // 0:ACTION 1:9999999 2:nickname 3:ident 4-N:action
                // *||Schooly_D balls out of control
                String[] parts = raw.split("\\s");
                String action = ArchivistUtil.compileMessage(parts, 4);
                return String.format(prettyFormat, stdSdf.format(new Date(Long.parseLong(parts[1]))), "*", parts[2], parts[3], action);
            }
        },
        NICK_CHANGE("%s is now known as %s") {
            @Override
            public String getPrettyFormatted(String raw) {
                // 0:NICK_CHANGE 1:9999999 2:oldnick 3:newnick
                // *||Schooly-D is now known as Schooly_D
                String[] parts = raw.split("\\s");
                return String.format(prettyFormat, stdSdf.format(new Date(Long.parseLong(parts[1]))), "*", parts[2], parts[3]);
            }
        },
        KICK("%s has kicked %s from %s (%s)") {
            @Override
            public String getPrettyFormatted(String raw) {
                // 0:KICK 1:9999999 2:kicker_nick 3:kickee_nick 4:channel 5-N:reason
                // *||Schooly_D has kicked NartFOpc from #tsd (lel)
                String[] parts = raw.split("\\s");
                String reason = ArchivistUtil.compileMessage(parts, 5);
                return String.format(prettyFormat, stdSdf.format(new Date(Long.parseLong(parts[1]))), "*", parts[2], parts[3], parts[4], reason);
            }
        };

        String prettyFormat;

        EventType(String prettyFormat) {
            this.prettyFormat = prettyPrefixFormat + prettyFormat;
        }

        public abstract String getPrettyFormatted(String raw);

        public static EventType getFromRaw(String raw) {
            for(EventType type : values()) {
                if(raw.startsWith(type.toString()))
                    return type;
            }
            return null;
        }
    }

}
