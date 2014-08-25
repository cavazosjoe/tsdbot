package org.tsd.tsdbot;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.tsd.tsdbot.util.IRCUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Joe on 2/26/14.
 */
public class HistoryBuff {

    private static HistoryBuff instance = null;
    private static final int CHANNEL_HISTORY_SIZE = 750; // 500 message history per channel
    private Map<String, CircularFifoBuffer> channelHistory = new HashMap<>();

    private HistoryBuff(String[] channels) {
        for(String channel : channels)
            channelHistory.put(channel, new CircularFifoBuffer(CHANNEL_HISTORY_SIZE));
    }

    public static HistoryBuff build(String [] channels) {
        if(instance == null) instance = new HistoryBuff(channels);
        return instance;
    }

    public static HistoryBuff getInstance() { return instance; }

    public synchronized void updateHistory(String channel, String message, String sender) {

        CircularFifoBuffer messages;

        if (channelHistory.containsKey(channel)) {
            messages = channelHistory.get(channel);
        } else {
            messages = new CircularFifoBuffer(CHANNEL_HISTORY_SIZE);
            channelHistory.put(channel, messages);
        }

        Message m = new Message();
        m.sender = sender;
        m.text = message;

        messages.add(m);

    }

    public List<Message> getMessagesByChannel(String channel, String targetUser) {
        LinkedList<Message> possibilities = new LinkedList<>();

        if (!channelHistory.containsKey(channel)) {
            return possibilities;
        }

        CircularFifoBuffer buffer = channelHistory.get(channel);

        if(targetUser == null) {
            // not filtering by user, dump everything
            for(Object o : buffer) {
                Message m = (Message)o;
                possibilities.addFirst(m);
            }
        } else {
            // fuzzy match on user handle
            possibilities = IRCUtil.fuzzySubset(targetUser, buffer, new IRCUtil.FuzzyVisitor<Message>() {
                @Override
                public String visit(Message o1) {
                    return o1.sender;
                }
            });
        }

        return possibilities;
    }

    public class Message {
        public String text;
        public String sender;
        //Date date?
    }
}
