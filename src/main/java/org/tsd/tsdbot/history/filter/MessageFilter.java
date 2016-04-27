package org.tsd.tsdbot.history.filter;

import org.tsd.tsdbot.history.HistoryBuff;

import java.util.HashSet;

public class MessageFilter {

    private HashSet<MessageFilterStrategy> filters = new HashSet<>();

    private MessageFilter() {}

    public MessageFilter addFilter(MessageFilterStrategy strategy) {
        filters.add(strategy);
        return this;
    }

    public boolean validateMessage(HistoryBuff.Message m) {
        for(MessageFilterStrategy strategy : filters) {
            if(!strategy.apply(m))
                return false;
        }
        return true;
    }

    public static MessageFilter create() {
        return new MessageFilter();
    }
}
