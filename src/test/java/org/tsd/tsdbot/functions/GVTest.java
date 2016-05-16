package org.tsd.tsdbot.functions;

import org.jibble.pircbot.User;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.TestBot2;
import org.tsd.tsdbot.TestBotModule;
import org.tsd.tsdbot.history.HistoryBuff;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

@RunWith(JukitoRunner.class)
public class GVTest {

    private static final String channel = "#tsd";

    private static final String notGv = "Not_GeeVee";
    private static final String normalUser = "Schooly_D";
    private static final String[] gvAliases = {
            "General_Vagueness", "GV"
    };

    @Test
    public void test(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;

        HashSet<String> gvResponses = new HashSet<>();
        gvResponses.addAll(Arrays.asList(GeeVee.gvResponses));

        sendMessage(bot, normalUser, ".gv");
        assertNull(testBot.getAllMessages(channel));

        testBot.reset();

        String msg = "I'm a big fan of Delta Force, and the delta WAS a triangle";
        sendMessage(bot, normalUser, msg);
        sendMessage(bot, normalUser, ".gv");

        List<String> sentMessages = testBot.getLastMessages(channel, 2);
        assertEquals("<Schooly_D> " + msg, sentMessages.get(1));
        assertTrue(gvResponses.contains(sentMessages.get(0)));

        String wellFormedSentence = "We hold these truths to be self-evident, that all men are created equal, " +
                "that they are endowed by their Creator with certain unalienable Rights, that among these are Life, " +
                "Liberty and the pursuit of Happiness.";
        String runOnSentence = "It kind of seems to me, and maybe to others too, although I haven't really asked " +
                "others, but it seems reasonable that they might also, maybe think, and I'm not really convinced of " +
                "this myself, but there might be a chance that I'm in love with a stripper with a heart of gold.";
        String fixedSentence = "It kind of seems to me. And maybe to others too. Although I haven't really asked " +
                "others. But it seems reasonable that they might also. Maybe think. And I'm not really convinced of " +
                "this myself. But there might be a chance that I'm in love with a stripper with a heart of gold.";

        for(String gvAlias : gvAliases) {
            testBot.reset();
            sendMessage(bot, notGv, wellFormedSentence);
            sendMessage(bot, gvAlias, runOnSentence);
            sendMessage(bot, normalUser, ".gv pls");
            assertEquals(fixedSentence, testBot.getLastMessage(channel));
        }
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
            testBot.addUser(IntegTestUtils.createUserWithPriv(normalUser, User.Priv.NONE), channel);
            testBot.addUser(IntegTestUtils.createUserWithPriv(notGv, User.Priv.NONE), channel);
            for(String nick : gvAliases) {
                testBot.addUser(IntegTestUtils.createUserWithPriv(nick, User.Priv.NONE), channel);
            }

            bind(TSDBot.class).toInstance(testBot);

            bind(HistoryBuff.class).asEagerSingleton();
            IntegTestUtils.loadFunctions(binder(), GeeVee.class);

            requestInjection(testBot);
        }
    }
}
