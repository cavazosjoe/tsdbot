package org.tsd.tsdbot.util;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Joe on 2/19/14.
 */
public class IRCUtil {

    public static String[] splitLongString(String input) {
        int len = input.length();
        if(len <= 510) return new String[]{input};
        LinkedList<String> retList = new LinkedList<>();
        int curIdx = 0;
        int cutoff = -1;
        while(curIdx < len) {
            cutoff = Math.min(len,curIdx+510);
            retList.addLast(input.substring(curIdx, cutoff));
            curIdx += 510;
        }
        return retList.toArray(new String[]{});
    }
}
