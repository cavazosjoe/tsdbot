package org.tsd.tsdbot.history;

import org.tsd.tsdbot.util.MiscUtils;

/**
 * Created by Joe on 1/14/2015.
 */
public class NoURLsStrategy implements MessageFilterStrategy {
    @Override
    public boolean apply(HistoryBuff.Message m) {
        return !m.text.matches(MiscUtils.URL_REGEX);
    }
}
