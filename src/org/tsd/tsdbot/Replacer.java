package org.tsd.tsdbot;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.tsd.tsdbot.HistoryBuff.*;
import org.tsd.tsdbot.util.IRCUtil;

public class Replacer {

    // Detects commands which look like s/find/replace/ username
    private static Pattern commandFormat = Pattern.compile("^s/([^/]+)/([^/]*)(.*)$");

    private HistoryBuff historyBuffer;

    public Replacer(HistoryBuff historyBuffer) {
        this.historyBuffer = historyBuffer;
    }

    public String tryStringReplace(String channel, String message) {
        return tryStringReplace(channel, message, null);
    }

    public String tryStringReplace(String channel, String message, String myName) {

        Matcher matcher = commandFormat.matcher(message);
        if (matcher.find()) {

            String find = matcher.group(1);
            String replace = matcher.group(2);
            String theRest = matcher.group(3);

            // Trim off any leading "/g" looking stuff that comes before the username
            String user = theRest.replaceFirst("^(/g ?|/)\\s*", "");

            List<Message> possibilities = historyBuffer.getFilteredPossibilities(channel, user);

            for (Message m: possibilities) {
                String replaced = replace(m.text, find, replace);
                if (replaced != null) {
                    return m.sender + " \u0002meant\u0002 to say: " + replaced;
                }
            }

            if (myName != null && IRCUtil.fuzzyMatches(user, myName)) {
                return "I said what I meant.";
            }

        }

        return null;
    }

    private static String replace(String message, String find, String replace) {

        String modified = message.replace(find, replace);

        if (!modified.equals(message)) {
            return modified;
        }

        return null;
    }

}
