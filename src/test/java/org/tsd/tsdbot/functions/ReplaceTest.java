package org.tsd.tsdbot.functions;

import org.jibble.pircbot.User;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.TestBot2;
import org.tsd.tsdbot.TestBotModule;
import org.tsd.tsdbot.history.HistoryBuff;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(JukitoRunner.class)
public class ReplaceTest {

    private static final String channel = "#tsd";

    private static final String testUser1 = "Schooly_D";
    private static final String testUser2 = "Schooly_B";

    @Test
    public void testReplaceLatestLine(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;

        sendMessage(bot, testUser1, "one two three four");
        sendMessage(bot, testUser1, "one two three four five");
        sendMessage(bot, testUser1, "s/four/FOUR");

        assertEquals(testUser1+" \u0002meant\u0002 to say: one two three FOUR five", testBot.getLastMessage(channel));
    }

    @Test
    public void testTargetedReplace(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;

        sendMessage(bot, testUser1, "a");
        sendMessage(bot, testUser2, "a");
        sendMessage(bot, testUser1, "s/a/b/"+testUser1);

        assertEquals(testUser1+" \u0002meant\u0002 to say: b", testBot.getLastMessage(channel));
    }

    @Test
    public void testNothingHappensWhenNoMatches(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;

        sendMessage(bot, testUser1, "a");
        sendMessage(bot, testUser1, "s/c/a");
        assertNull(testBot.getLastMessage(channel));
    }

    @Before
    public void reset(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;
        testBot.reset();
    }

    private void sendMessage(TSDBot bot, String user, String text) {
        bot.onMessage(channel, user, user.toLowerCase(), "hostname", text);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            install(new TestBotModule(channel));
            bind(Random.class).toInstance(new Random());

            TestBot2 testBot = new TestBot2();
            testBot.addUser(IntegTestUtils.createUserWithPriv(testUser1, User.Priv.NONE), channel);
            testBot.addUser(IntegTestUtils.createUserWithPriv(testUser2, User.Priv.NONE), channel);

            bind(TSDBot.class).toInstance(testBot);

            bind(HistoryBuff.class).asEagerSingleton();
            IntegTestUtils.loadFunctions(binder(), Replace.class);

            requestInjection(testBot);
        }
    }
}
