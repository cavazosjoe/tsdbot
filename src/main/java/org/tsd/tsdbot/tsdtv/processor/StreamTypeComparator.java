package org.tsd.tsdbot.tsdtv.processor;

import java.util.Comparator;

/**
 * Created by Joe on 2/28/2015.
 */
public class StreamTypeComparator implements Comparator<StreamType> {
    @Override
    public int compare(StreamType o1, StreamType o2) {
        return Integer.compare(o1.getOrder(), o2.getOrder());
    }
}
