package org.tsd.tsdbot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(JUnit4.class)
public class ReplacerTest {

    private Replacer replacer;

    @Before
    public void setup() {
        replacer = new Replacer();

        replacer.updateHistory("tsd", "This is a good test", "tarehart");
        replacer.updateHistory("tsd", "Some corgi stuff", "Tex");
        replacer.updateHistory("tsd", "A funny corgi joke", "Schooly_D");
        replacer.updateHistory("nomansland", "beep boop corgi", "Bot");
    }

    private String makeMessage(String replacement, String user) {
        return user + " \u0016meant\u000F to say: " + replacement;
    }

    @Test
    public void tryDifferentFormats() {

        String replacement = replacer.tryGetResponse("tsd", "s/good/superb/g");
        assertEquals(makeMessage("This is a superb test", "tarehart"), replacement);

        replacement = replacer.tryGetResponse("tsd", "s/good/superb/");
        assertEquals(makeMessage("This is a superb test", "tarehart"), replacement);

        replacement = replacer.tryGetResponse("tsd", "s/good/superb");
        assertEquals(makeMessage("This is a superb test", "tarehart"), replacement);

        replacement = replacer.tryGetResponse("tsd", "s/good/superb/tarehart");
        assertEquals(makeMessage("This is a superb test", "tarehart"), replacement);

        replacement = replacer.tryGetResponse("tsd", "s/good/superb/g tarehart");
        assertEquals(makeMessage("This is a superb test", "tarehart"), replacement);
    }

    @Test
    public void testRecentMessagePrecedence() {
        String replacement = replacer.tryGetResponse("tsd", "s/corgi/dog");
        assertEquals(makeMessage("A funny dog joke", "Schooly_D"), replacement);
    }

    @Test
    public void testCensorship() {
        String replacement = replacer.tryGetResponse("tsd", "s/corgi/ /Schooly_D");
        assertEquals(makeMessage("A funny   joke", "Schooly_D"), replacement);
    }

    @Test
    public void testRegex() {
        String replacement = replacer.tryGetResponse("tsd", "s/\\.*/dudes");
        assertNull(replacement); // No regex allowed.
    }

    @Test
    public void testCorrectSelf() {
        String replacement = replacer.tryGetResponse("nomansland", "s/something/dudes", "bot");
        assertEquals("I said what I meant.", replacement);
    }


}