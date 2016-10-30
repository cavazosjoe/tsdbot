package org.tsd.tsdbot.history;

import com.google.common.collect.EvictingQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.history.filter.MessageFilter;
import org.tsd.tsdbot.util.fuzzy.FuzzyLogic;
import org.tsd.tsdbot.util.fuzzy.FuzzyVisitor;

import java.util.*;

@Singleton
public class HistoryBuff {

    private static final int CHANNEL_HISTORY_SIZE = 750;
    private Random random;
    private Map<String, EvictingQueue<Message>> channelHistory = new HashMap<>();

    @Inject
    public HistoryBuff(TSDBot bot, Random random) {
        for(String channel : bot.getChannels())
            channelHistory.put(channel, EvictingQueue.create(CHANNEL_HISTORY_SIZE));
        this.random = random;
    }

    public synchronized void updateHistory(String channel, String message, String sender, MessageType type) {

        EvictingQueue<Message> messages;

        if (channelHistory.containsKey(channel)) {
            messages = channelHistory.get(channel);
        } else {
            messages = EvictingQueue.create(CHANNEL_HISTORY_SIZE);
            channelHistory.put(channel, messages);
        }

        Message m = new Message();
        m.sender = sender;
        m.text = message;
        m.type = type;

        messages.add(m);

    }

    public List<Message> getMessagesByChannel(String channel, String targetUser) {
        LinkedList<Message> possibilities = new LinkedList<>();

        if (!channelHistory.containsKey(channel)) {
            return possibilities;
        }

        EvictingQueue<Message> buffer = channelHistory.get(channel);

        if(targetUser == null) {
            // not filtering by user, dump everything
            for(Object o : buffer) {
                Message m = (Message)o;
                possibilities.addFirst(m);
            }
        } else {
            // fuzzy match on user handle
            possibilities = FuzzyLogic.fuzzySubset(targetUser, buffer, new FuzzyVisitor<Message>() {
                @Override
                public String visit(Message o1) {
                    return o1.sender;
                }
            });
        }

        return possibilities;
    }

    public Message getRandomFilteredMessage(String channel, String targetUser, MessageFilter filter) {
        List<Message> found = getRandomFilteredMessages(channel, targetUser, 1, filter);
        if(!found.isEmpty()) {
            return found.get(0);
        } else {
            return null;
        }
    }

    public LinkedList<Message> getRandomFilteredMessages(String channel, String targetUser, Integer num, MessageFilter filter) {
        if(num == null) num = Integer.MAX_VALUE; // no limit
        List<Message> history = getMessagesByChannel(channel, targetUser);
        LinkedList<Message> filteredHistory = new LinkedList<>();
        Message msg;
        while(!history.isEmpty() && filteredHistory.size() < num) {
            msg = history.get(random.nextInt(history.size()));
            if(filter == null || filter.validateMessage(msg)) {
                filteredHistory.add(msg);
            }
            history.remove(msg);
        }
        return filteredHistory;
    }

    public void reset() {
        for(EvictingQueue<Message> buffer : channelHistory.values()) {
            buffer.clear();
        }
    }

    public class Message {
        public String text;
        public String sender;
        public MessageType type;
        //Date date?
    }

    public enum MessageType {
        NORMAL,
        COMMAND,
        BLACKLISTED
    }
}
