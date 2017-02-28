package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.module.Function;
import org.tsd.tsdbot.util.fuzzy.FuzzyLogic;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@Function(initialRegex = "^s/.+?/[^/]*")
public class Replace extends MainFunctionImpl {

    private HistoryBuff historyBuff;

    // Detects commands which look like s/find/replace/ username
    private Pattern commandFormat = Pattern.compile("^s/([^/]+)/([^/]*)(.*)$");

    @Inject
    public Replace(TSDBot bot, HistoryBuff historyBuff) {
        super(bot);
        this.historyBuff = historyBuff;
        this.description = "Replace stuff";
        this.usage = "USAGE: s/text1/text2";
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {
        String replaceResult = tryStringReplace(channel, text);
        if(replaceResult != null) {
            bot.sendMessage(channel, replaceResult);
        }
    }

    private String tryStringReplace(String channel, String message) {
        return tryStringReplace(channel, message, null);
    }

    private String tryStringReplace(String channel, String message, String myName) {

        Matcher matcher = commandFormat.matcher(message);
        if (matcher.find()) {

            String find = matcher.group(1);
            String replace = matcher.group(2);
            String theRest = matcher.group(3);

            // Trim off any leading "/g" looking stuff that comes before the username
            String user = theRest.replaceFirst("^(/g ?|/)\\s*", "");

            List<HistoryBuff.Message> possibilities = historyBuff.getMessagesByChannel(channel, user);

            for (HistoryBuff.Message m : possibilities) {
                String replaced = replace(m.text, find, replace);
                if (replaced != null) {
                    return m.sender + " \u0002meant\u0002 to say: " + replaced;
                }
            }

            if (myName != null && FuzzyLogic.fuzzyMatches(user, myName)) {
                return "I said what I meant.";
            }
        }

        return null;
    }

    private String replace(String message, String find, String replace) {
        String modified = message.replace(find, replace);
        return modified.equals(message) ? null : modified;
    }

}
