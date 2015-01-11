package org.tsd.tsdbot.util;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

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

    public static String scrambleNick(String nick) {
        return HandleScramble.applyRandom(nick);
    }

    public static enum HandleScramble {
        vowel_flip { // flip one random vowel if available
            @Override
            public String apply(String input) {
                Random rand = new Random();
                LinkedList<Character> vowels = new LinkedList<>(Arrays.asList('a','e','i','o','u'));
                Collections.shuffle(vowels);

                LinkedList<Integer> vowelIdxs = new LinkedList<>();
                for(int i=0 ; i < input.length() ; i++) {
                    if(vowels.contains(input.charAt(i)))
                        vowelIdxs.addLast(i);
                }

                if(!vowelIdxs.isEmpty()) {
                    int c_idx = vowelIdxs.get(rand.nextInt(vowelIdxs.size()));
                    char c = input.charAt(c_idx);

                    for(Character v : vowels) {
                        if(c != v) {
                            String newString = input.substring(0, c_idx);
                            newString += v;
                            newString += input.substring(c_idx+1, input.length());
                            return newString;
                        }
                    }
                }

                return input;
            }
        };

        public abstract String apply(String input);

        public static String applyRandom(String input) {
            Random rand = new Random();
            return values()[rand.nextInt(values().length)].apply(input);
        }
    }

}
