package org.tsd.tsdbot.runnable;

import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;

import java.util.*;

/**
 * Created by Joe on 2/19/14.
 */
public class StrawPoll extends IRCListenerThread {

    private static Logger logger = LoggerFactory.getLogger("StrawPoll");

    private String proposer;
    private String question;
    private int duration; //minutes
    private TreeMap<Integer, String> optionsTable = new TreeMap<>(); // 1 -> choice1
    private HashSet<Vote> votes = new HashSet<>();
    private boolean aborted = false;

    public StrawPoll(TSDBot bot, String channel, String proposer, String question, int duration, String[] options) throws Exception {

        super(channel);

        this.bot = bot;
        this.proposer = proposer;

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

        if(options.length > 5)
            throw new Exception("Please limit your choices to 5");

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
        String[] displayTable = new String[3];
        displayTable[0] = "NEW STRAWPOLL: " + question;

        StringBuilder choicesBuilder = new StringBuilder();
        boolean first = true;
        for(Integer i : optionsTable.keySet()) {
            if(!first) choicesBuilder.append(" | ");
            choicesBuilder.append("<").append(i).append(": ").append(optionsTable.get(i)).append(">");
            first = false;
        }
        displayTable[1] = choicesBuilder.toString();
        displayTable[2] = "To vote, type '.vote <number of your choice>'. The voting will end in " + duration + " minute(s)";
        bot.sendMessages(channel,displayTable);
        startTime = System.currentTimeMillis();

        logger.info("BEGINNING STRAW POLL: {}, duration={}", question, duration);
    }

    private void handlePollResult() {

        if(aborted) {
            bot.sendMessage(channel, "The strawpoll has been canceled.");
            logger.info("STRAW POLL CANCELLED: {}", question);
            return;
        }

        final HashMap<String, Integer> results = new HashMap<>(); // choice -> numVotes
        for(String choice : optionsTable.values()) //initialize results
            results.put(choice,0);
        for(Vote vote : votes)
            results.put(vote.choice, results.get(vote.choice)+1);

        LinkedList<String> orderedKeys = new LinkedList<>(results.keySet());
        Collections.sort(orderedKeys, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return results.get(o2).compareTo(results.get(o1));
            }
        });

        boolean tie = results.get(orderedKeys.get(0)).equals(results.get(orderedKeys.get(1)));

        String[] resultsTable = new String[2];
        resultsTable[0] = question + " | RESULTS:";
        StringBuilder resultsBuilder = new StringBuilder();
        boolean first = true;
        for(String choice : orderedKeys) {
            if(!first) resultsBuilder.append(" | ");
            else {
                if(tie) resultsBuilder.append("TIE: ");
                else resultsBuilder.append("WINNER: ");
                first = false;
            }
            resultsBuilder.append(choice).append(", ").append(results.get(choice)).append(" votes");
        }
        resultsTable[1] = resultsBuilder.toString();
        bot.sendMessages(channel,resultsTable);
        logger.info("STRAW POLL ENDED: {}", question);
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
//            else  bot.sendMessage(channel,"Your vote has been counted, " + sender);

        } else if(command.equals(TSDBot.Command.STRAWPOLL)) {

            if(cmdParts.length != 2) return;

            synchronized (mutex) {
                if(cmdParts[1].equals("abort")) {
                    if(sender.equals(proposer) || bot.getUserFromNick(channel,sender).hasPriv(User.Priv.OP)) {
                        this.aborted = true;
                        mutex.notify();
                    } else {
                        bot.sendMessage(channel,"Only an op or the proposer can cancel this poll.");
                    }
                }
            }

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
        handlePollStart();
        synchronized (mutex) {
            try {
                mutex.wait(duration * 60 * 1000);
            } catch (InterruptedException e) {
                logger.info("StrawPoll.call() interrupted", e);
            }
        }
        handlePollResult();
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
