package org.tsd.tsdbot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Created by Joe on 4/5/14.
 */
public class GVUtil {

    private static Logger logger = LoggerFactory.getLogger("GVUtil");

    private static String[] gvResponses = new String[]{
            "I don't think you understand, but whatever",
            "Why?",
            "I don't think that's correct",
            "Why would you think that?",
            "I'm pretty sure I heard about that, but maybe not",
            "That doesn't really sound like something I'd like",
            "I'm not interested, sorry",
            "I don't really care, sorry",
            "Where did you see that?",
            "Where did you read that?",
            "Where did you hear that?",
            "I'm pretty sure science says no, but you can disagree I guess, maybe",
            "Why do you care?",
            "Why would you care?",
            "That's something I've always found interesting",
            "That's something I never found interesting, sorry",
            "My prof would probably disagree",
            "ur a faget",
            "I don't understand. Why?",
            "This isn't really something I want to discuss, sorry",
            "This isn't something I want to discuss, sorry",
            "I thought we agreed that was wrong?",
            "Why would you bring this up now?",
            "That's not very interesting",
            "Why am I supposed to care about that?",
            "I'd ask you to elaborate but I don't really care, sorry",
            "I'd ask you to elaborate but I'm not interested, sorry"
    };

    public static String getRandomGvResponse() {
        Random rand = new Random();
        return gvResponses[rand.nextInt(gvResponses.length)];
    }

    // This is a long sentence, it could be longer, but this is just a, you know, sentence
    // This is a long sentence. It could be longer. But this is just a. You know. Sentence
    public static String breakUpSentence(String sentence) {
        try {
            String[] clauses = sentence.split(", ");
            if(clauses.length > 3) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for(String s : clauses) {
                    if(!first) sb.append(" ");
                    String firstChar = s.substring(0,1);
                    sb.append(firstChar.toUpperCase()).append(s.substring(1));
                    if(!s.endsWith(".")) sb.append(".");
                    first = false;
                }
                return sb.toString();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
