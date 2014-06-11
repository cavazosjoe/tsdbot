package org.tsd.tsdbot.functions;

import org.jpaste.pastebin.Pastebin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import sun.awt.image.ImageWatched;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 6/4/14.
 */
public class Archivist /*Exedol*/ implements MainFunction {

    private static final Logger log = LoggerFactory.getLogger(Archivist.class);

    public static final String stdPfx = "%-13s%-15d%-16s";
    public static final SimpleDateFormat stdSdf = new SimpleDateFormat("[MMM dd HH:mm]");

    private static final Pattern generalPattern =
            Pattern.compile("^([A-Z_]+)\\s+([0-9]+)\\s+\\[.*?\\]\\s+(.*)",Pattern.DOTALL);

    private static final Pattern joinPattern =
            Pattern.compile(         "^JOIN\\s+([0-9]+)\\s+\\[.*?\\]\\s+\\*\\s([\\S]+)\\s\\((\\S+?)@.*",Pattern.DOTALL);

    private static final Pattern partPattern =
            Pattern.compile("^(?:PART|QUIT)\\s+([0-9]+)\\s+\\[.*?\\]\\s+\\*\\s([\\S]+)\\s\\((\\S+?)\\).*",Pattern.DOTALL);

    private static final long fiveMinutes = 1000 * 60 * 5;

    private static String archiveDir = null;
    private static String pastebinKey = null;

    private HashMap<String, Formatter> writerMap = new HashMap<>();

    public Archivist(Properties properties, String[] channels) throws IOException {

        stdSdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

        pastebinKey = properties.getProperty("pastebin.dev_key");
        archiveDir = properties.getProperty("archivist.logs");
        File logDirF = new File(archiveDir);
        if(!logDirF.exists()) {
            log.info("Logging directory {} does not exist, creating...", archiveDir);
            if(logDirF.mkdir())
                log.info("Logging directory {} successfully created", archiveDir);
            else
                log.warn("Logging directory {} WAS NOT created", archiveDir);
        }

        for(String channel : channels) {
            log.info("Adding channel {} to Archivist", channel);
            File f = new File(archiveDir + channel.replace("#","") + ".log");
            if(!f.exists()) {
                log.info("Logging file {} does not exist, creating...", f.getAbsolutePath());
                if(f.createNewFile())
                    log.info("Logging file {} successfully created", f.getAbsolutePath());
                else
                    log.warn("Logging file {} WAS NOT created", f.getAbsolutePath());
            }

            if(!channel.startsWith("#")) channel = "#" + channel;
            writerMap.put(channel, new Formatter(new FileWriter(f, true)));
        }
    }

    public void log(EventType eventType, String channel, Object... args) {

        List<Formatter> toWrite = new LinkedList<>();
        if(channel == null) {
            toWrite.addAll(writerMap.values());
        } else {
            Formatter f = writerMap.get(channel);
            if(f != null)
                toWrite.add(f);
            else
                log.warn("Could not find FileWriter for channel {}", channel);
        }

        for(Formatter f : toWrite) {
            f.format(stdPfx + eventType.getFormat() + System.getProperty("line.separator"), args);
            f.flush();
        }
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        TSDBot bot = TSDBot.getInstance();

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

                    if(line.startsWith(EventType.JOIN.toString())) {
                        // someone joined, analyze to see if it's our guy
                        Matcher m = joinPattern.matcher(line);
                        while(m.find()) {
                            String id = m.group(3);
                            if(ident.equals(id)) {
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
                        }
                    } else if(line.startsWith(EventType.PART.toString()) || line.startsWith(EventType.QUIT.toString())) {
                        // someone left, analyze to see if it's our guy
                        Matcher m = partPattern.matcher(line);
                        while(m.find()) {
                            String id = m.group(3);
                            if(ident.equals(id)) {
                                // it's a match, if we're not recording, start
                                // if we are recording, dump what's been captured -- the guy was here to see it anyway
                                if(recording) {
                                    captureBuffer.clear();
                                    capturedText.clear();
                                }
                                recording = true;
                            }
                        }
                    }

                    if(recording)
                        captureBuffer.addLast(line);

                    // get the message's time in a really lazy way
                    Long now = Long.parseLong(line.substring(13,26));
                    if(pastFiveMinutes.containsKey(now)) {
                        pastFiveMinutes.get(now).addLast(line);
                    } else {
                        LinkedList<String> m = new LinkedList<>();
                        m.add(line);
                        pastFiveMinutes.put(now, m);
                    }

                    pastFiveMinutes = trimFiveMinuteBuffer(pastFiveMinutes, now);

                }

                StringBuilder output = new StringBuilder();

                if(capturedText.isEmpty()) {
                    for(Long key : pastFiveMinutes.keySet()) {
                        for(String s : pastFiveMinutes.get(key)) {
                            log.info("-5MIN RECAP || " + s);
                            output.append(s).append(System.getProperty("line.separator"));
                        }
                    }
                } else {
                    for(String s : capturedText) {
                        log.info("RECAP || " + s);
                        output.append(s).append(System.getProperty("line.separator"));
                    }
                }

                if(!(output.toString().isEmpty())) {
                    URL url = Pastebin.pastePaste(pastebinKey, output.toString());
                    if(capturedText.isEmpty())
                        bot.sendMessage(channel, "I don't think you've missed anything. I'll recap the last five minutes");
                    bot.sendMessage(channel, getRawPastebinURL(url));
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
                        timestamp = Long.parseLong(line.substring(13,26));
                        if(recording || now - timestamp < 1000 * 60 * minutes) {
                            catchup.addLast(line);
                            recording = true;
                        }
                    }

                    StringBuilder output = new StringBuilder();
                    for(String s : catchup) {
                        output.append(s).append(System.getProperty("line.separator"));
                    }

                    URL url = Pastebin.pastePaste(pastebinKey, output.toString());
                    bot.sendMessage(channel, "Here are the chat logs from the past " + minutes + " minutes: " + getRawPastebinURL(url));

                } catch (Exception e) {
                    log.error("There was an error processing the channel archive", e);
                }

            } catch (NumberFormatException nfe) {
                // the second argument isn't a number
                bot.sendMessage(channel, TSDBot.Command.RECAP.getUsage());
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

    private String getRawPastebinURL(URL url) {
        String urlString = url.toString();
        String id = urlString.substring(urlString.lastIndexOf("/") + 1);
        return "http://pastebin.com/raw.php?i=" + id;
    }

    public static enum EventType {
        JOIN("* %s (%s@%s) has joined %s"),
        PART("* %s (%s) has left %s"),
        QUIT("* %s (%s) has quit (%s)"),
        MESSAGE("%-10s%20s||%s"),
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
