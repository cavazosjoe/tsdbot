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

import static org.junit.Assert.assertEquals;

@RunWith(JukitoRunner.class)
public class BlacklistTest {

    private static final String channel = "#tsd";
    private static final String owner = IntegTestUtils.BOT_OWNER;
    private static final String notOwner = "Schooly_B";

    @Test
    public void testAddAndRemoveFromBlacklist(TSDBot bot) {

        TestBot2 testBot = (TestBot2)bot;

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
    public void testNonOwnerCannotBlacklist(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;
        sendMessage(bot, notOwner, ".blacklist add "+owner);
        assertEquals("Only my owner can banish people to the shadow realm", testBot.getLastMessage(channel));
    }

    @Test
    public void testNoFunctionsForBlacklistedUser(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;
        sendMessage(bot, owner, ".blacklist add "+notOwner);
        sendMessage(bot, notOwner, "hey there, this here's some text");
        sendMessage(bot, notOwner, "s/e/a");
        assertEquals("Schooly_B has been sent to the shadow realm", testBot.getLastMessage(channel));
        sendMessage(bot, owner, "s/e/a");
        assertEquals("Schooly_B \u0002meant\u0002 to say: hay thara, this hara's soma taxt", testBot.getLastMessage(channel));
    }

    @Before
    public void resetBot(TSDBot bot) {
        ((TestBot2)bot).reset();
    }

    private void sendMessage(TSDBot bot, String user, String text) {
        bot.onMessage(channel, user, user.toLowerCase(), "hostname", text);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            install(new TestBotModule(channel));
            TestBot2 bot = new TestBot2();
            bind(TSDBot.class).toInstance(bot);
            IntegTestUtils.loadFunctions(binder(), Blacklist.class, Replace.class);


            bot.addUser(IntegTestUtils.createUserWithPriv(owner, User.Priv.NONE), channel);
            bot.addUser(IntegTestUtils.createUserWithPriv(notOwner, User.Priv.NONE), channel);

            requestInjection(bot);
        }
    }
}
