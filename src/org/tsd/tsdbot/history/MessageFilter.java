package org.tsd.tsdbot.history;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Joe on 9/4/2014.
 */
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
