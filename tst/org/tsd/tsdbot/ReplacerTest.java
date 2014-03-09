package org.tsd.tsdbot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(JUnit4.class)
public class ReplacerTest {
    
    HistoryBuff historyBuff;

    @Before
    public void setup() {
        historyBuff = new HistoryBuff();

        historyBuff.updateHistory("tsd", "This is a good test", "tarehart");
        historyBuff.updateHistory("tsd", "Some corgi stuff", "Tex");
        historyBuff.updateHistory("tsd", "A funny corgi joke", "Schooly_D");
        historyBuff.updateHistory("nomansland", "beep boop corgi", "Bot");
    }

    private String makeMessage(String replacement, String user) {
        return user + " \u0016meant\u000F to say: " + replacement;
    }

    @Test
    public void tryDifferentFormats() {

        String replacement = Replacer.tryStringReplace("tsd", "s/good/superb/g", historyBuff);
        assertEquals(makeMessage("This is a superb test", "tarehart"), replacement);

        replacement = Replacer.tryStringReplace("tsd", "s/good/superb/", historyBuff);
        assertEquals(makeMessage("This is a superb test", "tarehart"), replacement);

        replacement = Replacer.tryStringReplace("tsd", "s/good/superb", historyBuff);
        assertEquals(makeMessage("This is a superb test", "tarehart"), replacement);

        replacement = Replacer.tryStringReplace("tsd", "s/good/superb/tarehart", historyBuff);
        assertEquals(makeMessage("This is a superb test", "tarehart"), replacement);

        replacement = Replacer.tryStringReplace("tsd", "s/good/superb/g tarehart", historyBuff);
        assertEquals(makeMessage("This is a superb test", "tarehart"), replacement);
    }

    @Test
    public void testRecentMessagePrecedence() {
        String replacement = Replacer.tryStringReplace("tsd", "s/corgi/dog", historyBuff);
        assertEquals(makeMessage("A funny dog joke", "Schooly_D"), replacement);
    }

    @Test
    public void testCensorship() {
        String replacement = Replacer.tryStringReplace("tsd", "s/corgi/ /Schooly_D", historyBuff);
        assertEquals(makeMessage("A funny   joke", "Schooly_D"), replacement);
    }

    @Test
    public void testRegex() {
        String replacement = Replacer.tryStringReplace("tsd", "s/\\.*/dudes", historyBuff);
        assertNull(replacement); // No regex allowed.
    }

    @Test
    public void testCorrectSelf() {
        String replacement = Replacer.tryStringReplace("nomansland", "s/something/dudes", "bot", historyBuff);
        assertEquals("I said what I meant.", replacement);
    }


}