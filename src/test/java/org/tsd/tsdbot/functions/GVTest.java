package org.tsd.tsdbot.functions;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TestBot;
import org.tsd.tsdbot.history.HistoryBuff;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JukitoRunner.class)
public class GVTest {

    private static final String channel = "#tsd";

    @Test
    public void test(Bot bot) {
        TestBot testBot = (TestBot)bot;

        HashSet<String> gvResponses = new HashSet<>();
        gvResponses.addAll(Arrays.asList(GeeVee.gvResponses));

        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", ".gv");
        assertEquals(0, testBot.getAllMessages(channel).size());

        testBot.reset();

        String msg = "I'm a big fan of Delta Force, and the delta WAS a triangle";
        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", msg);
        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", ".gv");

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

        String[] gvAliases = {
                "General_Vagueness", "general_vagueness", "gv", "GV"
        };
        for(String gvAlias : gvAliases) {
            testBot.reset();
            bot.onMessage(channel, "Not_GeeVee", "notgv", "hostname", wellFormedSentence);
            bot.onMessage(channel, gvAlias, "ident", "hostname", runOnSentence);
            bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", ".gv pls");
            assertEquals(fixedSentence, testBot.getLastMessage(channel));
        }

    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            bind(Random.class).toInstance(new Random());

            TestBot testBot = new TestBot(channel);
            bind(Bot.class).toInstance(testBot);

            bind(HistoryBuff.class).asEagerSingleton();
            IntegTestUtils.loadFunctions(binder(), GeeVee.class);

            requestInjection(testBot);
        }
    }
}
