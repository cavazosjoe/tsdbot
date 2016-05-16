package org.tsd.tsdbot.functions;

import org.jibble.pircbot.User;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.*;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JukitoRunner.class)
public class BlunderCountTest {

    private static final String channel = "#tsd";
    private static final String user = "Schooly_D";
    private static FauxRandom random;

    @Test
    public void testBlunderCount(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;

        assertEquals(0, testBot.getBlunderCount());
        sendMessage(bot, user, ".blunder count");
        assertTrue(testBot.getLastMessage(channel).toLowerCase().startsWith("current blunder count"));

        random.setDoubleVal(1.0);
        sendMessage(bot, user, ".blunder +");
        assertEquals(1, testBot.getBlunderCount());
        String lastMessage = testBot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().contains("incremented to 1"));
    }

    private void sendMessage(TSDBot bot, String user, String text) {
        bot.onMessage(channel, user, user.toLowerCase(), "hostname", text);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            install(new TestBotModule(channel));
            random = new FauxRandom();
            bind(Random.class).toInstance(random);

            TestBot2 testBot = new TestBot2();
            testBot.addUser(IntegTestUtils.createUserWithPriv(user, User.Priv.NONE), channel);
            bind(TSDBot.class).toInstance(testBot);

            IntegTestUtils.loadFunctions(binder(), BlunderCount.class);
        }
    }
}
