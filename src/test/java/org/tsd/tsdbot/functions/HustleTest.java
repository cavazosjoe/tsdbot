package org.tsd.tsdbot.functions;

import com.google.inject.name.Names;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TestBot;
import org.tsd.tsdbot.stats.HustleStats;

import static org.junit.Assert.assertEquals;

/**
 * Created by Joe on 4/1/2015.
 */
@RunWith(JukitoRunner.class)
public class HustleTest {

    private static final String channel = "#tsd";

    @Test
    public void test(Bot bot) {
        TestBot testBot = (TestBot)bot;

        testBot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", ".hustle");
        String lastMessage = testBot.getLastMessage(channel);
        assertEquals("Current HHR: 0.50 -- http://localhost/hustle", lastMessage);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            bind(String.class).annotatedWith(Names.named("serverUrl")).toInstance("http://localhost");

            HustleStats hustleStats = Mockito.mock(HustleStats.class);
            Mockito.when(hustleStats.getHhr()).thenReturn(0.50);
            bind(HustleStats.class).toInstance(hustleStats);

            TestBot testBot = new TestBot(channel, null);
            bind(Bot.class).toInstance(testBot);

            IntegTestUtils.loadFunctions(binder(), Hustle.class);

            requestInjection(testBot);
        }
    }
}
