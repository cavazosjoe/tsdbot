package org.tsd.tsdbot.functions;

import org.jibble.pircbot.User;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TestBot;
import org.tsd.tsdbot.module.BotOwner;

@RunWith(JukitoRunner.class)
public class BlacklistTest {

    private static final String channel = "#tsd";
    private static final String owner = "Schooly_D";
    private static final String notOwner = "Schooly_B";

    @Test
    public void testAddAndRemoveFromBlacklist(Bot bot) {
        TestBot testBot = (TestBot)bot;

        sendMessage(bot, owner, ".blacklist add "+notOwner);
        assertEquals(notOwner+" has been sent to the shadow realm", testBot.getLastMessage(channel));

        sendMessage(bot, owner, ".blacklist add "+notOwner);
        assertEquals(notOwner+" was already in the shadow realm", testBot.getLastMessage(channel));

        sendMessage(bot, owner, ".blacklist remove "+notOwner);
        assertEquals(notOwner+" has been freed from the shadow realm", testBot.getLastMessage(channel));

        sendMessage(bot, owner, ".blacklist remove "+notOwner);
        assertEquals(notOwner+" was not in the shadow realm", testBot.getLastMessage(channel));
    }

    @Test
    public void testNonOwnerCannotBlacklist(Bot bot) {
        TestBot testBot = (TestBot)bot;

        sendMessage(bot, notOwner, ".blacklist add "+owner);
        assertEquals("Only my owner can banish people to the shadow realm", testBot.getLastMessage(channel));
    }

    @Test
    public void testNoFunctionsForBlacklistedUser(Bot bot) {
        TestBot testBot = (TestBot)bot;

        sendMessage(bot, owner, ".blacklist add "+notOwner);
        sendMessage(bot, notOwner, "hey there, this here's some text");
        sendMessage(bot, notOwner, "s/e/a");
        assertEquals("Only my owner can banish people to the shadow realm", testBot.getLastMessage(channel));
    }

    private void sendMessage(Bot bot, String user, String text) {
        bot.onMessage(channel, user, user.toLowerCase(), "hostname", text);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            TestBot testBot = new TestBot(channel);
            bind(Bot.class).toInstance(testBot);
            IntegTestUtils.loadFunctions(binder(), Blacklist.class, Replace.class);
            bind(String.class).annotatedWith(BotOwner.class).toInstance(owner);

            testBot.addChannelUser(channel, User.Priv.NONE, owner);
            testBot.addChannelUser(channel, User.Priv.NONE, notOwner);

            requestInjection(testBot);
        }
    }
}
