package org.tsd.tsdbot.markov;

import org.apache.commons.lang3.StringUtils;
import org.tsd.tsdbot.util.MarkovUtil;

import java.util.Arrays;

public class MarkovKey {

    public String[] words;

    public MarkovKey(MarkovKey old, String nextWord) {
        words = new String[old.words.length];
        System.arraycopy(old.words, 1, this.words, 0, old.words.length - 1);
        this.words[words.length-1] = MarkovUtil.sanitize(nextWord);
    }

    public MarkovKey(String... words) {
        this.words = new String[words.length];
        for (int i=0 ; i < words.length ; i++) {
            this.words[i] = MarkovUtil.sanitize(words[i]);
        }
    }

    public String words() {
        return StringUtils.join(words, " ");
    }

    @Override
    public String toString() {
        return String.format("[%s]", words());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MarkovKey markovKey = (MarkovKey) o;

        return Arrays.equals(words, markovKey.words);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(words);
    }
}
