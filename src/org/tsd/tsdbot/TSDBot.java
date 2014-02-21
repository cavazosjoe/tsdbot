package org.tsd.tsdbot;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jibble.pircbotm.PircBot;
import org.tsd.tsdbot.runnable.Strawpoll;
import org.tsd.tsdbot.util.IRCUtil;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Joe on 2/18/14.
 */
public class TSDBot extends PircBot implements Runnable {

    private static String chan;
    
    private Thread mainThread;

    private HboForumManager hboForumManager;
    private DboForumManager dboForumManager;

    private ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private Strawpoll runningPoll;

    public TSDBot(String channel, String name) {
        chan = channel;

        setName(name);
        setAutoNickChange(true);
        setLogin("tsdbot");

        CloseableHttpClient httpClient = HttpClients.createMinimal();

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getCookieManager().setCookiesEnabled(true);

        hboForumManager = new HboForumManager(httpClient);
        dboForumManager = new DboForumManager(webClient);

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
            case STRAWPOLL: poll(message.split(";")); break;
            case VOTE: vote(sender, login, cmdParts); break;
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

    private void poll(String[] cmdParts) {
        if(runningPoll != null) {
            sendMessage(chan,"There is already a poll running. It will end in " + runningPoll.getMinutes() + " minute(s)");
            return;
        }

        if(cmdParts.length < 4) {
            sendMessage(chan,".poll usage: .poll <question> ; <duration (integer)> ; choice 1 ; choice 2 [ choice 3 ...]");
            return;
        }

        String question = cmdParts[0].substring(cmdParts[0].indexOf(" ") + 1).trim(); //  .poll This is a question
                                                                                      //        ^--->
        int minutes = Integer.parseInt(cmdParts[1].trim());
        String[] choices = new String[cmdParts.length-2];
        for(int i=0 ; i < choices.length ; i++) {
            choices[i] = cmdParts[i+2].trim();
        }

        try {
            runningPoll = new Strawpoll(
                    this,
                    question,
                    minutes,
                    choices
            );
            threadPool.submit(runningPoll);
        } catch (Exception e) {
            sendMessage(chan,e.getMessage());
        }
    }

    private void vote(String sender, String login, String[] cmdParts) {
        if(runningPoll == null) {
            sendMessage(chan,"There is no poll running");
            return;
        }

        if(cmdParts.length != 2) {
            sendMessage(chan,".vote <number of your choice>");
            return;
        }

        int selection = -1;
        try {
            selection = Integer.parseInt(cmdParts[1]);
        } catch (NumberFormatException nfe) {
            sendMessage(chan,".vote <number of your choice>");
            return;
        }

        String voteResult = runningPoll.castVote(login, selection);
        if(voteResult != null) {
            sendMessage(chan, voteResult + ", " + sender);
        }
    }

    public void handlePollStart(String question, int minutes, TreeMap<Integer, String> choices) {
        String[] displayTable = new String[choices.size()+2];
        displayTable[0] = "NEW STRAWPOLL: " + question;
        for(Integer i : choices.keySet()) {
            displayTable[i] = i + ": " + choices.get(i);
        }
        displayTable[displayTable.length-1] = "The voting will end in " + minutes + " minute(s)";
        sendLines(displayTable);
    }

    public void handlePollResult(String question, HashMap<String, Integer> results) {
        String[] resultsTable = new String[results.size()+1];
        resultsTable[0] = question + " | RESULTS:";
        int i=1;
        for(String choice : results.keySet()) {
            resultsTable[i] = choice + ": " + results.get(choice);
            i++;
        }
        this.runningPoll = null;
        sendLines(resultsTable);
    }

    @Override
    public synchronized void run() {

        LinkedList<HboForumManager.HboForumPost> hboForumNotifications;
        LinkedList<DboForumManager.DboForumPost> dboForumNotifications;

        while(true) {
            try {
                wait(120 * 1000); // check every 2 minutes
            } catch (InterruptedException e) {
                // something notified this thread, panic.blimp
            }

            hboForumNotifications = hboForumManager.sweep();
            for(HboForumManager.HboForumPost post : hboForumNotifications) {
                sendLine(post.getInline());
            }

            dboForumNotifications = dboForumManager.sweep();
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
        DBO_FORUM(".dbof"),
        STRAWPOLL(".poll"),
        VOTE(".vote");

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
