package org.tsd.tsdbot.warzone;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.warzone.library.IntroParams;
import org.tsd.tsdbot.warzone.library.Library;

import java.util.HashMap;

import static org.junit.Assert.assertNotNull;

@RunWith(JukitoRunner.class)
public class LibraryTest {

    @Test
    public void testIntro(Library library) {

        HashMap<String, Object> params = new HashMap<>();
        params.put(IntroParams.TOTAL_WINS, 10);
        params.put(IntroParams.TOTAL_LOSSES, 20);
        params.put(IntroParams.TEAM_NAME, "Schooly D and the Wild Bunch");
        IntroParams introParams = new IntroParams(params);

        String intro = library.getIntroLibrary().getIntroSentence(introParams);
        assertNotNull(intro);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {

        }
    }

}
