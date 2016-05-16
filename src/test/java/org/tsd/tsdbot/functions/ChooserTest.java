package org.tsd.tsdbot.functions;

import org.apache.commons.lang3.RandomStringUtils;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.FauxRandom;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.TestBot2;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JukitoRunner.class)
@Ignore
public class ChooserTest {

    private static String channel = "#tsd";
    private static FauxRandom random;

    @Test
    public void testChooser(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;

        String lastMessage;
        String[] choices = {"one", "two", "three"};
        for(int i=0 ; i < choices.length ; i++) {
            random.setIntVal(i);

            testBot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", ".choose one|two|three");
            lastMessage = testBot.getLastMessage(channel);
            assertTrue(lastMessage.endsWith(choices[i]));

            testBot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", ".choose one   |two|     three");
            lastMessage = testBot.getLastMessage(channel);
            assertTrue(lastMessage.endsWith(choices[i]));
        }

        testBot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", ".choose one");
        lastMessage = testBot.getLastMessage(channel);
        assertTrue(lastMessage.endsWith("bro"));
    }

    @Test
    public void testGvSchroogle(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;

        String okay = RandomStringUtils.randomAlphanumeric(10);
        String tooLong = RandomStringUtils.randomAlphanumeric(20);

        String choiceFmt = ".choose %s|%s|%s";

        String okayChoices = String.format(choiceFmt, okay, okay, okay);
        String tooLongChoices = String.format(choiceFmt, okay, tooLong, okay);

        String[] gvNicks = {"General_Vagueness", "GV", "gv", "G_Vague"};
        for(String nick : gvNicks) {

            testBot.onMessage(channel, nick, "gv", "hostname", okayChoices);
            String lastMessage = testBot.getLastMessage(channel);
            assertTrue(lastMessage.endsWith(okay));

            testBot.onMessage(channel, nick, "gv", "hostname", tooLongChoices);
            lastMessage = testBot.getLastMessage(channel);
            assertFalse(lastMessage.contains(okay) || lastMessage.contains(tooLong));

        }
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            random = new FauxRandom();
            bind(Random.class).toInstance(random);

            TestBot2 testBot = new TestBot2();
            bind(TSDBot.class).toInstance(testBot);

            IntegTestUtils.loadFunctions(binder(), Chooser.class);

            requestInjection(testBot);
        }
    }
}
