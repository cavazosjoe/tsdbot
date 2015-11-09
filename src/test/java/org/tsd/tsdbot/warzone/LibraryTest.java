package org.tsd.tsdbot.warzone;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertNotNull;

public class LibraryTest {

    private static final Library library = new Library();

    @Test
    public void testIntro() {

        HashMap<String, Object> params = new HashMap<>();
        params.put("totalWins", 10);
        params.put("totalLosses", 20);
        params.put("teamName", "Schooly D and the Wild Bunch");

        String intro = library.introSentences.pop().gen(new Library.IntroParams(params));
        assertNotNull(intro);
    }

}
