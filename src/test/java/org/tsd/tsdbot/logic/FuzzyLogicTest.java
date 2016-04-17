package org.tsd.tsdbot.logic;

import org.junit.Test;
import org.tsd.tsdbot.util.fuzzy.FuzzyLogic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Joe on 3/25/2015.
 */
public class FuzzyLogicTest {

    @Test
    public void testFuzzyMatch() {

        String[][] match = new String[][]{
                {null, null},
                {"", ""},
                {"A", "A"},
                {"a", "A"},
                {"a", "aaa"},
                {"a", "AAA"},
                {"something", "something else"},
                {"  ", "something  else"},
                {"1.001", "1.0011"},
                {"ippo", "IPpo's LONG STR0ng DONG ATTACK!!!"}
        };

        String[][] noMatch = new String[][]{
                {null, ""},
                {"", null},
                {"a", "e"},
                {"a", "eee"},
                {" ", ""},
                {"something", "somethin"},
                {"2", "between two ferns"},
                {"  _  ", "_"}
        };

        for(String[] pair : match) {
            assertTrue(FuzzyLogic.fuzzyMatches(pair[0], pair[1]));
        }

        for(String[] pair : noMatch) {
            assertFalse(FuzzyLogic.fuzzyMatches(pair[0], pair[1]));
        }
    }
}
