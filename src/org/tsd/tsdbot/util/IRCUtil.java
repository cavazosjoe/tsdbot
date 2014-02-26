package org.tsd.tsdbot.util;

import java.util.LinkedList;

import static com.rosaloves.bitlyj.Bitly.*;

/**
 * Created by Joe on 2/19/14.
 */
public class IRCUtil {

    private static final int MAX_MSG_LEN = 510;

    public static String[] splitLongString(String input) {
        int len = input.length();
        if(len <= MAX_MSG_LEN) return new String[]{input};
        LinkedList<String> retList = new LinkedList<>();
        int curIdx = 0;
        int cutoff = -1;
        while(curIdx < len) {
            cutoff = Math.min(len,curIdx+MAX_MSG_LEN);
            retList.addLast(input.substring(curIdx, cutoff));
            curIdx += MAX_MSG_LEN;
        }
        return retList.toArray(new String[]{});
    }

    public static String trimToSingleMsg(String input) {
        input = input.replaceAll("\n"," ").replaceAll("\r","");
        int len = input.length();
        if(len <= MAX_MSG_LEN) return input;
        else return input.substring(0,MAX_MSG_LEN-3) + "...";
    }

    public static String shortenUrl(String url) {
        return as("o_181ooefbmh","R_7adf723e32a4493b92bf9014439137a6")
                .call(shorten(url)).getShortUrl();
    }

    public static boolean fuzzyMatches(String targetUser, String sender) {
        return sender.toLowerCase().startsWith(targetUser.toLowerCase());
    }
}
