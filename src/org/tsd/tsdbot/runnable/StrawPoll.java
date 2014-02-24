package org.tsd.tsdbot.runnable;

import org.tsd.tsdbot.TSDBot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Created by Joe on 2/19/14.
 */
public class StrawPoll extends IRCListenerThread {

    private String question;
    private int duration; //minutes
    private TreeMap<Integer, String> optionsTable = new TreeMap<>(); // 1 -> choice1
    private HashSet<Vote> votes = new HashSet<>();

    public StrawPoll(TSDBot bot, String channel, ThreadManager threadManager, String question, int duration, String[] options) throws Exception {

        super(threadManager,channel);

        this.bot = bot;

        listeningCommands.add(TSDBot.Command.STRAWPOLL);
        listeningCommands.add(TSDBot.Command.VOTE);

        if(question == null || question.isEmpty())
            throw new Exception("Question cannot be blank");
        if(!question.endsWith("?"))
            throw new Exception("You must phrase your question in the form of a question");
        this.question = question;

        if(duration < 1 || duration > 5)
            throw new Exception("Minutes must be a whole number between 1 and 5 (inclusive)");
        this.duration = duration;

        int i=1;
        for(String o : options) {
            if(o == null || o.isEmpty())
                throw new Exception("Answer choice " + i + " is blank");
            if(optionsTable.containsValue(o))
                throw new Exception("Duplicate answer choice: " + o);
            optionsTable.put(i,o);
            i++;
        }

    }

    public String castVote(String voter, Integer choice) {
        if(!optionsTable.containsKey(choice)) return choice + " is not a valid choice";
        if(!(votes.add(new Vote(voter,optionsTable.get(choice))))) return "You can't vote twice";
        return null;
    }

    private void handlePollStart() {
        String[] displayTable = new String[optionsTable.size()+2];
        displayTable[0] = "NEW STRAWPOLL: " + question;
        for(Integer i : optionsTable.keySet()) {
            displayTable[i] = i + ": " + optionsTable.get(i);
        }
        displayTable[displayTable.length-1] = "The voting will end in " + duration + " minute(s)";
        bot.sendMessages(channel,displayTable);
        startTime = System.currentTimeMillis();
    }

    private void handlePollResult(HashMap<String, Integer> results) {
        String[] resultsTable = new String[results.size()+1];
        resultsTable[0] = question + " | RESULTS:";
        int i=1;
        for(String choice : results.keySet()) {
            resultsTable[i] = choice + ": " + results.get(choice);
            i++;
        }
        bot.sendMessages(channel,resultsTable);
    }

    @Override
    public TSDBot.ThreadType getThreadType() {
        return TSDBot.ThreadType.STRAWPOLL;
    }

    @Override
    public void onMessage(TSDBot.Command command, String sender, String login, String hostname, String message) {
        if(!listeningCommands.contains(command)) return;
        String[] cmdParts = message.split("\\s+");

        if(command.equals(TSDBot.Command.VOTE)) {

            if(cmdParts.length != 2) {
                bot.sendMessage(channel,command.getUsage());
                return;
            }

            int selection = -1;
            try {
                selection = Integer.parseInt(cmdParts[1]);
            } catch (NumberFormatException nfe) {
                bot.sendMessage(channel,command.getUsage());
                return;
            }

            String voteResult = castVote(login, selection);
            if(voteResult != null) bot.sendMessage(channel,voteResult + ", " + sender);
            else  bot.sendMessage(channel,"Your vote has been counted, " + sender);

        }

    }

    @Override
    public void onPrivateMessage(TSDBot.Command command, String sender, String login, String hostname, String message) {

    }

    @Override
    public long getRemainingTime() {
        return (duration * 60 * 1000) - (System.currentTimeMillis() - startTime);
    }

    @Override
    public Object call() throws Exception {
        System.out.println("starting poll");
        handlePollStart();
        synchronized (mutex) {
            try {
                mutex.wait(duration * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("poll over!");
        HashMap<String, Integer> results = new HashMap<>(); // choice -> numVotes TODO: ORDER THIS
        for(String choice : optionsTable.values()) { //initialize results
            results.put(choice,0);
        }

        for(Vote vote : votes) {
            results.put(vote.choice, results.get(vote.choice)+1);
        }
        handlePollResult(results);
        manager.removeThread(this);
        return null;
    }

    public class Vote {
        public String voter;
        public String choice;

        public Vote(String voter, String choice) {
            this.voter = voter;
            this.choice = choice;
        }

        @Override
        public boolean equals(Object that) {
            if(that == null) return false;
            if(this == that) return true;
            if(! (that instanceof Vote) ) return false;
            Vote thatVote = (Vote)that;
            return thatVote.voter.equals(this.voter);
        }

        @Override
        public int hashCode() {
            return voter.hashCode();
        }
    }
}
