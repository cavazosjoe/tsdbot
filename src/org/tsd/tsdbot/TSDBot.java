package org.tsd.tsdbot;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jibble.pircbotm.PircBot;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.util.LinkedList;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBot extends PircBot implements Runnable {

    private static final String chan = "#tsd";
    
    private Thread mainThread;

    private CloseableHttpClient httpClient;

    private HboForumManager hboForumManager;

    public TSDBot() {
        setName("TSDBot");
        setAutoNickChange(true);
        setLogin("tsdbot");

        httpClient = HttpClients.createMinimal();

        hboForumManager = new HboForumManager();

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
            case HBO_FORUM:
                if(cmdParts.length == 1) {
                    sendMessage(chan,".hbof usage: .hbof [ list | pv [postId (optional)] ]");
                } else if(cmdParts[1].equals("list")) {
                    for(HboForumManager.HboForumPost post : hboForumManager.history()) {
                        sendMessage(chan,post.getInline());
                    }
                } else if(cmdParts[1].equals("pv")) {
                    if(hboForumManager.history().isEmpty()) {
                        sendMessage(chan,"No HBO Forum threads in recent history");
                    } else if(cmdParts.length == 2) {
                        sendMessage(chan,hboForumManager.history().getFirst().getPreview());
                    } else {
                        try {
                            int postId = Integer.parseInt(cmdParts[2]);
                            boolean found = false;
                            for(HboForumManager.HboForumPost post : hboForumManager.history()) {
                                if(post.getPostId() == postId) {
                                    sendMessage(chan, post.getPreview());
                                    found = true;
                                    break;
                                }
                            }
                            if(!found) sendMessage(chan,"Could not find HBO Forum thread with ID " + postId + " in recent history");
                        } catch (NumberFormatException nfe) {
                            sendMessage(chan,cmdParts[2] + " does not appear to be a number");
                        }
                    }
                }
                break;
            case TOM_CRUISE:
                if(cmdParts.length == 1) {
                    sendMessage(chan,TomCruise.getRandom());
                } else if(cmdParts[1].equals("quote")) {
                    sendMessage(chan,TomCruise.getRandomQuote());
                } else if(cmdParts[1].equals("clip")) {
                    sendMessage(chan,TomCruise.getRandomClip());
                } else {
                    sendMessage(chan,".tc usage: .tc [ clip | quote ] (optional)");
                }
                break;
        }

    }

    @Override
    public synchronized void run() {

        LinkedList<HboForumManager.HboForumPost> hboForumNotifications;

        while(true) {
            try {
                wait(120 * 1000); // check every 2 minutes
            } catch (InterruptedException e) {
                // something notified this thread, panic.blimp
            }

            hboForumNotifications = hboForumManager.sweep(httpClient);
            for(HboForumManager.HboForumPost post : hboForumNotifications) {
                sendMessage(chan,post.getInline());
            }

        }
    }

    public enum Action {
        TOM_CRUISE(".tc"),
        HBO_FORUM(".hbof");

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
