package org.tsd.tsdbot.history.filter;

import org.tsd.tsdbot.history.HistoryBuff;

import java.util.HashSet;
import java.util.Set;

public class MessageFilter {

    private Set<MessageFilterStrategy> filters = new HashSet<>();

    private MessageFilter() {}

    public MessageFilter addFilter(MessageFilterStrategy strategy) {
        filters.add(strategy);
        return this;
    }

    public boolean validateMessage(HistoryBuff.Message m) {
        return filters.stream().allMatch(filter -> filter.apply(m));
    }

    public static MessageFilter create() {
        return new MessageFilter();
    }
}
