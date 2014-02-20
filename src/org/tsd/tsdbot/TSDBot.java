package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jibble.pircbotm.PircBot;
import org.tsd.tsdbot.util.IRCUtil;

import java.util.LinkedList;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBot extends PircBot implements Runnable {

    private static String chan;
    
    private Thread mainThread;

    private CloseableHttpClient httpClient;
    private WebClient webClient;

    private HboForumManager hboForumManager;
    private DboForumManager dboForumManager;

    public TSDBot(String channel, String name) {
        chan = channel;

        setName(name);
        setAutoNickChange(true);
        setLogin("tsdbot");

        httpClient = HttpClients.createMinimal();

        webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getCookieManager().setCookiesEnabled(true);

        hboForumManager = new HboForumManager();
        dboForumManager = new DboForumManager();

        mainThread = new Thread(this);
        mainThread.start();
    }

    @Override
    public synchronized void onMessage(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(".")) return; //not a command, ignore
        String[] cmdParts = message.split("\\s+");

        Action action = Action.fromString(cmdParts[0]);
        if(action == null) return;

        switch(action) {
            case HBO_FORUM: hbof(cmdParts); break;
            case DBO_FORUM: dbof(cmdParts); break;
            case TOM_CRUISE: tc(cmdParts); break;
        }

    }

    private void hbof(String[] cmdParts) {
        if(cmdParts.length == 1) {
            sendLine(".hbof usage: .hbof [ list | pv [postId (optional)] ]");
        } else if(cmdParts[1].equals("list")) {
            for(HboForumManager.HboForumPost post : hboForumManager.history()) {
                sendLine(post.getInline());
            }
        } else if(cmdParts[1].equals("pv")) {
            if(hboForumManager.history().isEmpty()) {
                sendLine("No HBO Forum threads in recent history");
            } else if(cmdParts.length == 2) {
                sendLines(hboForumManager.history().getFirst().getPreview());
            } else {
                try {
                    int postId = Integer.parseInt(cmdParts[2]);
                    boolean found = false;
                    for(HboForumManager.HboForumPost post : hboForumManager.history()) {
                        if(post.getPostId() == postId) {
                            sendLines(post.getPreview());
                            found = true;
                            break;
                        }
                    }
                    if(!found) sendLine("Could not find HBO Forum thread with ID " + postId + " in recent history");
                } catch (NumberFormatException nfe) {
                    sendLine(cmdParts[2] + " does not appear to be a number");
                }
            }
        }
    }

    private void dbof(String[] cmdParts) {
        if(cmdParts.length == 1) {
            sendLine(".dbof usage: .dbof [ list | pv [postId (optional)] ]");
        } else if(cmdParts[1].equals("list")) {
            for(DboForumManager.DboForumPost post : dboForumManager.history()) {
                sendLine(post.getInline());
            }
        } else if(cmdParts[1].equals("pv")) {
            if(dboForumManager.history().isEmpty()) {
                sendLine("No HBO Forum threads in recent history");
            } else if(cmdParts.length == 2) {
                sendLines(dboForumManager.history().getFirst().getPreview());
            } else {
                try {
                    int postId = Integer.parseInt(cmdParts[2]);
                    boolean found = false;
                    for(DboForumManager.DboForumPost post : dboForumManager.history()) {
                        if(post.getPostId() == postId) {
                            sendLines(post.getPreview());
                            found = true;
                            break;
                        }
                    }
                    if(!found) sendLine("Could not find DBO Forum thread with ID " + postId + " in recent history");
                } catch (NumberFormatException nfe) {
                    sendLine(cmdParts[2] + " does not appear to be a number");
                }
            }
        }
    }

    private void tc(String[] cmdParts) {
        if(cmdParts.length == 1) {
            sendLine(TomCruise.getRandom());
        } else if(cmdParts[1].equals("quote")) {
            sendLine(TomCruise.getRandomQuote());
        } else if(cmdParts[1].equals("clip")) {
            sendLine(TomCruise.getRandomClip());
        } else {
            sendLine(".tc usage: .tc [ clip | quote ] (optional)");
        }
    }

    @Override
    public synchronized void run() {

        LinkedList<HboForumManager.HboForumPost> hboForumNotifications;
        LinkedList<DboForumManager.DboForumPost> dboForumNotifications;

        while(true) {
            try {
                wait(30 * 1000); // check every 2 minutes
            } catch (InterruptedException e) {
                // something notified this thread, panic.blimp
            }

            hboForumNotifications = hboForumManager.sweep(httpClient);
            for(HboForumManager.HboForumPost post : hboForumNotifications) {
                sendLine(post.getInline());
            }

            dboForumNotifications = dboForumManager.sweep(webClient);
            for(DboForumManager.DboForumPost post : dboForumNotifications) {
                sendLine(post.getInline());
            }

        }
    }
    
    private void sendLine(String line) {
        if(line.length() > 510) sendLines(IRCUtil.splitLongString(line));
        else sendMessage(chan, line);
    }
    
    private void sendLines(String[] lines) {
        for(String line : lines) sendMessage(chan, line);
    }

    public enum Action {
        TOM_CRUISE(".tc"),
        HBO_FORUM(".hbof"),
        DBO_FORUM(".dbof");

        public String cmd;

        Action(String cmd) {
            this.cmd = cmd;
        }

        public static Action fromString(String s) {
            for(Action a : values()) {
                if(a.cmd.equals(s)) return a;
            }
            return null;
        }
    }
}
