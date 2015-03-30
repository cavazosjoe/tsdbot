package org.tsd.tsdbot.functions;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.FauxRandom;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TestBot;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by Joe on 3/29/2015.
 */
@RunWith(JukitoRunner.class)
public class BlunderCountTest {

    private static final String channel = "#tsd";
    private static FauxRandom random;

    @Test
    public void testBlunderCount(Bot bot) {
        TestBot testBot = (TestBot)bot;

        assertEquals(0, testBot.getBlunderCount());
        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", ".blunder count");
        assertTrue(testBot.getLastMessage(channel).toLowerCase().startsWith("current blunder count"));

        random.setDoubleVal(1.0);
        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", ".blunder +");
        assertEquals(1, testBot.getBlunderCount());
        String lastMessage = testBot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().contains("incremented to 1"));
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            random = new FauxRandom();
            bind(Random.class).toInstance(random);

            TestBot testBot = new TestBot(channel, null);
            bind(Bot.class).toInstance(testBot);

            IntegTestUtils.loadFunctions(binder(), BlunderCount.class);
        }
    }
}
