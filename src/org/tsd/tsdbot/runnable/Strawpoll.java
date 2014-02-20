package org.tsd.tsdbot.runnable;

import org.tsd.tsdbot.TSDBot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/**
 * Created by Joe on 2/19/14.
 */
public class Strawpoll implements Runnable {

    private TSDBot bot;

    private String question;
    private int minutes;
    private TreeMap<Integer, String> optionsTable = new TreeMap<>(); // 1 -> choice1
    private HashSet<Vote> votes = new HashSet<>();

    public Strawpoll(TSDBot bot, String question, int minutes, String[] options) throws Exception {

        this.bot = bot;

        if(question == null || question.isEmpty())
            throw new Exception("Question cannot be blank");
        if(!question.endsWith("?"))
            throw new Exception("You must phrase your question in the form of a question");
        this.question = question;

        if(minutes < 1 || minutes > 5)
            throw new Exception("Minutes must be a whole number between 1 and 5 (inclusive)");
        this.minutes = minutes;

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

    @Override
    public void run() {
        System.out.println("starting poll");
        bot.handlePollStart(question, minutes, optionsTable);
        try {
            Thread.sleep(minutes * 60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("poll over!");
        HashMap<String, Integer> results = new HashMap<>(); // choice -> numVotes TODO: ORDER THIS
        for(String choice : optionsTable.values()) { //initialize results
            results.put(choice,0);
        }

        for(Vote vote : votes) {
            results.put(vote.choice, results.get(vote.choice)+1);
        }
        bot.handlePollResult(question, results);
    }

    public int getMinutes() {
        return minutes;
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
