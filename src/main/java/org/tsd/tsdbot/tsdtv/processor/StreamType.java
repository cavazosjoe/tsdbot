package org.tsd.tsdbot.tsdtv.processor;

import org.tsd.tsdbot.tsdtv.TSDTVFileProcessor;

/**
* Created by Joe on 2/28/2015.
*/
public enum StreamType {
    VIDEO(0),
    AUDIO(1),
    SUBTITLE(2);

    private int order;

    StreamType(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public static StreamType fromString(String s) {
        for(StreamType type : StreamType.values()) {
            if(type.toString().compareToIgnoreCase(s) == 0)
                return type;
        }
        return null;
    }
}
