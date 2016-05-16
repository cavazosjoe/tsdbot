package org.tsd.tsdbot.functions;

import org.jibble.pircbot.User;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.TestBot2;
import org.tsd.tsdbot.TestBotModule;
import org.tsd.tsdbot.stats.HustleStats;

import static org.junit.Assert.assertEquals;

@RunWith(JukitoRunner.class)
public class HustleTest {

    private static final String channel = "#tsd";

    @Test
    public void test(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;

        testBot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", ".hustle");
        String lastMessage = testBot.getLastMessage(channel);
        assertEquals("Current HHR: 0.50 -- http://irc.org/hustle", lastMessage);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            install(new TestBotModule(channel));

            HustleStats hustleStats = Mockito.mock(HustleStats.class);
            Mockito.when(hustleStats.getHhr()).thenReturn(0.50);
            bind(HustleStats.class).toInstance(hustleStats);

            TestBot2 testBot = new TestBot2();
            testBot.addUser(IntegTestUtils.createUserWithPriv("Schooly_D", User.Priv.NONE), channel);
            bind(TSDBot.class).toInstance(testBot);

            IntegTestUtils.loadFunctions(binder(), Hustle.class);

            requestInjection(testBot);
        }
    }
}
