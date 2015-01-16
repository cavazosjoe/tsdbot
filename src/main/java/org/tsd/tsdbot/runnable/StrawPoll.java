package org.tsd.tsdbot.runnable;

import com.google.inject.Inject;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.ThreadType;
import org.tsd.tsdbot.functions.StrawPollFunction;

import java.util.*;

/**
 * Created by Joe on 2/19/14.
 */
public class StrawPoll extends IRCListenerThread {

    private static Logger logger = LoggerFactory.getLogger(StrawPoll.class);

    private String proposer;
    private String question;
    private int duration; //minutes
    private TreeMap<Integer, String> optionsTable = new TreeMap<>(); // 1 -> choice1
    private HashSet<Vote> votes = new HashSet<>();
    private boolean aborted = false;

    @Inject
    public StrawPoll(TSDBot bot, ThreadManager threadManager) throws Exception {
        super(bot, threadManager);
        this.listeningRegex = "^\\.(poll|vote).*";
    }

    public void init(String channel, String proposer, String question, int duration, String[] options) throws Exception {
        this.channel = channel;
        this.proposer = proposer;

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

    public void castVote(String voter, Integer choice) throws InvalidChoiceException, DuplicateVoteException {
        if(!optionsTable.containsKey(choice))
            throw new InvalidChoiceException();

        boolean voteResult = votes.add(new Vote(voter,optionsTable.get(choice)));
        if(!voteResult)
            throw new DuplicateVoteException();
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
    public ThreadType getThreadType() {
        return ThreadType.STRAWPOLL;
    }

    @Override
    public void onMessage(String sender, String login, String hostname, String message) {

        StrawPollOperation pollOp = StrawPollOperation.fromString(message);
        if(pollOp == null)
            return;

        logger.info("Received message: {} -> pollOp = {}", message, pollOp);

        String[] cmdParts = message.split("\\s+");

        if(pollOp.equals(StrawPollOperation.vote)) {

            if(cmdParts.length != 2) {
                bot.sendMessage(channel, ".vote [number of your choice]");
                return;
            }

            int selection = -1;
            try {
                selection = Integer.parseInt(cmdParts[1]);
            } catch (NumberFormatException nfe) {
                logger.warn("NFE", nfe);
                bot.sendMessage(channel, ".vote [number of your choice]");
                return;
            }

            try{
                castVote(login, selection);
                bot.sendMessage(channel, "Your vote has been counted, " + sender);
            } catch (DuplicateVoteException e) {
                bot.sendMessage(channel, "You can't vote twice, " + sender);
            } catch (InvalidChoiceException e) {
                bot.sendMessage(channel, "That is not a valid choice, " + sender);
            }

        } else if(pollOp.equals(StrawPollOperation.abort)) {

            if(sender.equals(proposer) || bot.getUserFromNick(channel, sender).hasPriv(User.Priv.OP)) {
                this.aborted = true;
                synchronized (mutex) {
                    mutex.notify();
                }
            } else {
                bot.sendMessage(channel, "Only an op or the proposer can cancel this poll.");
            }

        }

    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message) {}

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

    public enum StrawPollOperation {
        vote(".vote "),
        abort(".poll abort");

        StrawPollOperation(String prefix) {
            this.prefix = prefix;
        }

        private String prefix;

        public static StrawPollOperation fromString(String s) {
            for(StrawPollOperation op : values()) {
                if(s.startsWith(op.prefix))
                    return op;
            }
            return null;
        }
    }

    class InvalidChoiceException extends Exception {}
    class DuplicateVoteException extends Exception {}
}
