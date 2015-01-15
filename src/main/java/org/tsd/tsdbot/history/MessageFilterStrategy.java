package org.tsd.tsdbot.history;

/**
 * Created by Joe on 9/4/2014.
 */
public interface MessageFilterStrategy {
    public boolean apply(HistoryBuff.Message m);
}
