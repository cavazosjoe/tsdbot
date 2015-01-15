package org.tsd.tsdbot.history;

/**
 * Created by Joe on 1/14/2015.
 */
public class LengthStrategy implements MessageFilterStrategy {

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
