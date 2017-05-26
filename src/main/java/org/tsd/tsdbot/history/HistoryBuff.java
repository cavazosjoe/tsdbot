package org.tsd.tsdbot.history;

import com.google.common.collect.EvictingQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.history.filter.MessageFilter;
import org.tsd.tsdbot.util.fuzzy.FuzzyLogic;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class HistoryBuff {

    private static final Logger log = LoggerFactory.getLogger(HistoryBuff.class);

    private static final int CHANNEL_HISTORY_SIZE = 750;
    private Random random;
    private Map<String, EvictingQueue<Message>> channelHistory = new HashMap<>();

    @Inject
    public HistoryBuff(TSDBot bot, Random random) {
        for(String channel : bot.getChannels()) {
            channelHistory.put(channel, EvictingQueue.create(CHANNEL_HISTORY_SIZE));
        }
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
        m.date = Instant.now();

        messages.add(m);
    }

    public List<Message> getMessagesByChannel(String channel, String author) {
        log.info("Getting messages for channel {} with author {}", channel, author);

        if (!channelHistory.containsKey(channel)) {
            log.warn("Channel {} does not exist in history", channel);
            return Collections.emptyList();
        }

        List<Message> possibilities = new LinkedList<>();
        EvictingQueue<Message> buffer = channelHistory.get(channel);

        if(StringUtils.isBlank(author)) {
            log.debug("No author specified, returning all messages in channel");
            possibilities.addAll(buffer);
        } else {
            log.debug("Author specified, returning all messages matching author: {}", author);
            possibilities = FuzzyLogic.fuzzySubset(author, buffer, message -> message.sender);
        }

        Collections.reverse(possibilities);
        return possibilities.parallelStream()
                .filter(message -> message.type.equals(MessageType.NORMAL))
                .collect(Collectors.toList());
    }

    public Message getRandomFilteredMessage(String channel, String targetUser, MessageFilter filter) {
        List<Message> found = getRandomFilteredMessages(channel, targetUser, 1, filter);
        return found.isEmpty() ? null : found.get(0);
    }

    public Stack<Message> getRandomFilteredMessages(String channel, String targetUser, Integer num, MessageFilter filter) {
        if(num == null) {
            num = Integer.MAX_VALUE; // no limit
        }
        List<Message> history = getMessagesByChannel(channel, targetUser);
        Stack<Message> filteredHistory = new Stack<>();
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
        channelHistory.values().forEach(EvictingQueue::clear);
    }

    public class Message {
        public String text;
        public String sender;
        public MessageType type;
        public Instant date;

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("date", date)
                    .append("sender", sender)
                    .append("type", type)
                    .append("text", text)
                    .toString();
        }
    }

    public enum MessageType {
        NORMAL,
        COMMAND,
        BLACKLISTED
    }
}
