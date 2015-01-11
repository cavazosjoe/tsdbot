package org.tsd.tsdbot.functions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.history.HistoryBuff;
import org.tsd.tsdbot.history.MessageFilter;
import org.tsd.tsdbot.history.MessageFilterStrategy;
import org.tsd.tsdbot.util.IRCUtil;

import java.util.*;

/**
 * Created by Joe on 5/24/14.
 */
@Singleton
public class ScareQuote extends MainFunction {

    //TODO: put more than 5 minutes of effort into this and stop trying to "fix" it while drunk

    private HistoryBuff historyBuff;
    private Random random;

    @Inject
    public ScareQuote(TSDBot bot, HistoryBuff historyBuff, Random random) {
        super(bot);
        this.historyBuff = historyBuff;
        this.random = random;
    }

    @Override
    public void run(String channel, String sender, String ident, String text) {

        HistoryBuff.Message chosen = historyBuff.getRandomFilteredMessage(
                channel,
                null,
                MessageFilter.create()
                        .addFilter(new MessageFilterStrategy.NoCommandsStrategy())
                        .addFilter(new MessageFilterStrategy.LengthStrategy(null, 100))
                        .addFilter(new MessageFilterStrategy() {
                            @Override
                            public boolean apply(HistoryBuff.Message m) {
                                String[] w = m.text.split("\\s+");
                                return w.length > 2;
                            }
                        })
                        .addFilter(new MessageFilterStrategy() { // make sure at least one word is longer than 1 char
                            @Override
                            public boolean apply(HistoryBuff.Message m) {
                                String[] words = m.text.split("\\s+");
                                for (String word : words) {
                                    if (word.length() > 1 && (!isThrowaway(word)))
                                        return true;
                                }
                                return false;
                            }
                        })
        );

        if(chosen != null) {

            LinkedList<String> words = new LinkedList<>(Arrays.asList(chosen.text.split("\\s+")));

            // used so we can keep track of where a word is in a sentence even when it appears twice
            HashMap<Integer, String> wordMap = new HashMap<>();

            for(int i=0 ; i < words.size() ; i++) {
                wordMap.put(i, words.get(i));
            }

            String scary = null;        // this is the word we want to quote-ify
            Integer scary_idx = null;   // this is the index of the word in the original sentence
            while(scary == null && (!wordMap.isEmpty())) {
                int idxToCheck = random.nextInt(wordMap.size()); // check a random item in the hashmap
                Iterator it = wordMap.keySet().iterator();
                Integer idx;
                String word;
                for(int i=0 ; it.hasNext() ; i++) {
                    idx = (Integer) it.next();      // advance the cursor
                    if(i == idxToCheck) {           // check this word
                        word = wordMap.get(idx);
                        if (word.length() < 2 || isThrowaway(word)) {
                            it.remove();            // remove this word from the map if it's no good
                        } else {
                            scary = word;           // this random word is 2 characters or more, choose it
                            scary_idx = idx;
                        }
                    }
                }
            }

            if(scary == null)
                return;

            scary = scary.replaceAll("\"",""); //sanitize any quotes
            scary = "\"" + scary + "\"";

            StringBuilder result = new StringBuilder();
            int i = 0;
            for(String w : words) {
                if(i > 0) result.append(" ");
                if(i == scary_idx) result.append(scary);
                else result.append(w);
                i++;
            }

            bot.sendMessage(channel, "<" + IRCUtil.scrambleNick(chosen.sender) + "> " + result.toString());
        }
    }

    private boolean isThrowaway(String word) {
        for(String ta : throwawayWords) {
            if(ta.equalsIgnoreCase(word))
                return true;
        }
        return false;
    }

    // TODO: replace with a dictionary lib
    private static String[] throwawayWords = new String[]{
            "i",    "a",        "an",
            "to",   "and",      "the",
            "l'll", "can't",    "for",
            "too"
    };

}
