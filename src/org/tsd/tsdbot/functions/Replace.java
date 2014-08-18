package org.tsd.tsdbot.functions;

import org.tsd.tsdbot.HistoryBuff;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.util.IRCUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Joe on 5/24/14.
 */
public class Replace extends MainFunction {

    // Detects commands which look like s/find/replace/ username
    private Pattern commandFormat = Pattern.compile("^s/([^/]+)/([^/]*)(.*)$");

    @Override
    public void run(String channel, String sender, String ident, String text) {

        HistoryBuff historyBuff = HistoryBuff.getInstance();
        TSDBot bot = TSDBot.getInstance();

        String replaceResult = tryStringReplace(channel, text, historyBuff);
        if(replaceResult != null)
            bot.sendMessage(channel, replaceResult);
    }

    public String tryStringReplace(String channel, String message, HistoryBuff historyBuffer) {
        return tryStringReplace(channel, message, null, historyBuffer);
    }

    public String tryStringReplace(String channel, String message, String myName, HistoryBuff historyBuffer) {

        Matcher matcher = commandFormat.matcher(message);
        if (matcher.find()) {

            String find = matcher.group(1);
            String replace = matcher.group(2);
            String theRest = matcher.group(3);

            // Trim off any leading "/g" looking stuff that comes before the username
            String user = theRest.replaceFirst("^(/g ?|/)\\s*", "");

            List<HistoryBuff.Message> possibilities = historyBuffer.getMessagesByChannel(channel, user);

            for (HistoryBuff.Message m: possibilities) {
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

    private String replace(String message, String find, String replace) {

        String modified = message.replace(find, replace);

        if (!modified.equals(message)) {
            return modified;
        }

        return null;
    }
}
