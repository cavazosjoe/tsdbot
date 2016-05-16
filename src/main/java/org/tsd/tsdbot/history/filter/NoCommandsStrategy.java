package org.tsd.tsdbot.history.filter;

import org.tsd.tsdbot.history.HistoryBuff;

public class NoCommandsStrategy implements MessageFilterStrategy {
    @Override
    public boolean apply(HistoryBuff.Message m) {
        return m.type.equals(HistoryBuff.MessageType.NORMAL);
    }
}
