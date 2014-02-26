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

    private static final int CHANNEL_HISTORY_SIZE = 20; // 20 message history per channel
    private Map<String, CircularFifoBuffer> channelHistory = new HashMap<>();

    public void initialize(String[] channels) {
        for(String channel : channels)
            channelHistory.put(channel, new CircularFifoBuffer(CHANNEL_HISTORY_SIZE));
    }

    public void updateHistory(String channel, String message, String sender) {

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

    public List<Message> getFilteredPossibilities(String channel, String targetUser) {
        LinkedList<Message> possibilities = new LinkedList<>();

        if (!channelHistory.containsKey(channel)) {
            return possibilities;
        }

        CircularFifoBuffer messages = channelHistory.get(channel);

        for (Object o: messages) {
            Message m = (Message) o;
            if (targetUser == null || IRCUtil.fuzzyMatches(targetUser, m.sender)) {
                possibilities.addFirst(m);
            }
        }

        return possibilities;
    }

    public class Message {
        String text;
        String sender;
        //Date date?
    }
}
