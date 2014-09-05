package org.tsd.tsdbot.history;

import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.util.MiscUtils;

/**
 * Created by Joe on 9/4/2014.
 */
public interface MessageFilterStrategy {

    public boolean apply(HistoryBuff.Message m);

    public static class NoCommandsStrategy implements MessageFilterStrategy {
        @Override
        public boolean apply(HistoryBuff.Message m) {
            return TSDBot.Command.fromString(m.text).size() == 0;
        }
    }

    public static class NoURLsStrategy implements MessageFilterStrategy {
        @Override
        public boolean apply(HistoryBuff.Message m) {
            return !m.text.matches(MiscUtils.URL_REGEX);
        }
    }

    public static class LengthStrategy implements MessageFilterStrategy {

        private int minLen = Integer.MIN_VALUE;
        private int maxLen = Integer.MAX_VALUE;

        public LengthStrategy(Integer minLen, Integer maxLen) {
            if(minLen != null) this.minLen = minLen;
            if(maxLen != null) this.maxLen = maxLen;
        }

        @Override
        public boolean apply(HistoryBuff.Message m) {
            int len = m.text.length();
            return len >= minLen && len <= maxLen;
        }
    }
}
