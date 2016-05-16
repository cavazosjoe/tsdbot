package org.tsd.tsdbot.functions;

import org.jibble.pircbot.User;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.*;
import org.tsd.tsdbot.history.HistoryBuff;

import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JukitoRunner.class)
public class DeejTest {

    private static final String channel = "#tsd";
    private static final String user = "Schooly_D";

    @Before
    public void resetBot(TSDBot bot) {
        ((TestBot2) bot).reset();
    }

    @Test
    public void testDeej(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;

        sendMessage(bot, user, ".deej");
        assertNull(testBot.getAllMessages(channel));

        // test that the "no commands" filter works
        sendMessage(bot, user, ".fname");
        sendMessage(bot, user, ".quote");
        sendMessage(bot, user, ".gv");
        sendMessage(bot, user, ".odb get");
        sendMessage(bot, user, ".tsdtv play ippo random");
        sendMessage(bot, user, ".deej");
        assertNull(testBot.getAllMessages(channel));

        sendMessage(bot, user, ".muh nert");
        sendMessage(bot, user, ".deej");
        String lastMessage = testBot.getLastMessage(channel);
        assertTrue(lastMessage.contains("muh nert"));
    }

    private void sendMessage(TSDBot bot, String user, String text) {
        bot.onMessage(channel, user, user.toLowerCase(), "hostname", text);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            install(new TestBotModule(channel));
            Random random = new FauxRandom();
            bind(Random.class).toInstance(random);

            TestBot2 testBot = new TestBot2();
            testBot.addUser(IntegTestUtils.createUserWithPriv(user, User.Priv.NONE), channel);
            bind(TSDBot.class).toInstance(testBot);

            bind(HistoryBuff.class).asEagerSingleton();

            HashSet<Class<? extends MainFunction>> functionsToInject = new HashSet<>();
            functionsToInject.add(Deej.class);
            install(new MockFunctionModule(functionsToInject));

            requestInjection(testBot);
        }
    }

}
