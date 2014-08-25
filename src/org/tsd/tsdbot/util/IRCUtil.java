package org.tsd.tsdbot.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.LinkedList;
import java.util.List;

import static com.rosaloves.bitlyj.Bitly.as;
import static com.rosaloves.bitlyj.Bitly.shorten;

/**
 * Created by Joe on 2/19/14.
 */
public class IRCUtil {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String BOLD_CHAR = "\u0002";

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
        input = StringEscapeUtils.unescapeXml(input);
        input = input.replaceAll("\n"," ").replaceAll("\r","");
        int len = input.length();
        if(len <= MAX_MSG_LEN) return input;
        else return input.substring(0,MAX_MSG_LEN-3) + "...";
    }

    public static String shortenUrl(String url) {
        return as("o_181ooefbmh","R_7adf723e32a4493b92bf9014439137a6")
                .call(shorten(url)).getShortUrl();
    }

    public static boolean fuzzyMatches(String query, String element) {
        if(query == null && element == null) return true;
        else if(query == null || element == null) return false;
        else return element.toLowerCase().contains(query.toLowerCase());
    }

    public static <T> LinkedList<T> fuzzySubset(String query, Iterable<T> choices, FuzzyVisitor<T> visitor) {
        LinkedList<T> ret = new LinkedList<>();
        for(T choice : choices) {
            if(fuzzyMatches(query, visitor.visit(choice)))
                ret.addFirst(choice);
        }
        return ret;
    }

    public static String getRandomString() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    public static interface FuzzyVisitor<T> {
        public String visit(T o1);
    }

}
