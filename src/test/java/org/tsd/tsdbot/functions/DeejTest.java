package org.tsd.tsdbot.functions;

import com.google.inject.multibindings.Multibinder;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.*;
import org.tsd.tsdbot.history.HistoryBuff;

import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by Joe on 3/27/2015.
 */
@RunWith(JukitoRunner.class)
public class DeejTest {

    private static Random random;
    private static final String channel = "#tsd";

    @Before
    public void resetBot(Bot bot) {
        ((TestBot) bot).reset();
    }

    @Test
    public void testDeej(Bot bot) {
        TestBot testBot = (TestBot)bot;

        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", ".deej");
        assertEquals(0, testBot.getAllMessages(channel).size());

        // test that the "no commands" filter works
        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", ".fname");
        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", ".quote");
        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", ".gv");
        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", ".odb get");
        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", ".tsdtv play ippo random");
        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", ".deej");
        assertEquals(0, testBot.getAllMessages(channel).size());

        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", "muh nert");
        testBot.onMessage(channel, "Schooly_D", "schoolyd", "host", ".deej");
        String lastMessage = testBot.getLastMessage(channel);
        assertTrue(lastMessage.contains("muh nert"));
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            random = new FauxRandom();
            bind(Random.class).toInstance(random);

            TestBot testBot = new TestBot(channel, null);
            bind(Bot.class).toInstance(testBot);

            bind(HistoryBuff.class).asEagerSingleton();

            HashSet<Class<? extends MainFunction>> functionsToInject = new HashSet<>();
            functionsToInject.add(Deej.class);
            install(new MockFunctionModule(functionsToInject));

            requestInjection(testBot);
        }
    }

}
