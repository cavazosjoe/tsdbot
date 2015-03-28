package org.tsd.tsdbot.integ;

import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.jibble.pircbot.User;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.PrintoutLibrary;
import org.tsd.tsdbot.TestBot;
import org.tsd.tsdbot.functions.Printout;

import java.io.FileNotFoundException;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by Joe on 3/26/2015.
 */
@RunWith(JukitoRunner.class)
public class PrintoutTest {

    private static final String server = "http://test";
    private static final String channel = "#tsd";

    private static FauxRandom random;

    @Test
    public void testNormal(Injector injector) throws FileNotFoundException {
        Printout printoutFunction = injector.getInstance(Printout.class);
        TestBot bot = (TestBot) injector.getInstance(Bot.class);
        PrintoutLibrary library = injector.getInstance(PrintoutLibrary.class);

        random.setDoubleVal(1.0);
        printoutFunction.run(channel, "Schooly_D", "schoolyd", "TSDBot I need a printout of a big ol' fuzzy bear");
        String lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.startsWith(server));
        String id = lastMessage.substring(lastMessage.lastIndexOf("/") + 1);
        byte[] printout = library.getPrintout(id);
        assertNotNull(printout);
        assertTrue(printout.length > 0);
    }

    @Test
    public void testRepeat(Injector injector) throws FileNotFoundException {

        Printout printoutFunction = injector.getInstance(Printout.class);
        TestBot bot = (TestBot) injector.getInstance(Bot.class);
        PrintoutLibrary library = injector.getInstance(PrintoutLibrary.class);

        random.setDoubleVal(0);
        printoutFunction.run(channel, "Schooly_D", "schoolyd", "TSDBot I need a printout of a big ol' fuzzy bear");
        String lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("not computing"));
        printoutFunction.run(channel, "Schooly_D", "schoolyd", "BIG FUZZY BEAR");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("not computing"));

        printoutFunction.run(channel, "Schooly_D", "schoolyd", "BIG. FUZZY. BEAR.");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.startsWith(server));
        String id = lastMessage.substring(lastMessage.lastIndexOf("/") + 1);
        byte[] printout = library.getPrintout(id);
        assertNotNull(printout);
        assertTrue(printout.length > 0);
    }

    @Test
    public void testBlacklist(Injector injector) throws FileNotFoundException {
        Printout printoutFunction = injector.getInstance(Printout.class);
        TestBot bot = (TestBot) injector.getInstance(Bot.class);
        PrintoutLibrary library = injector.getInstance(PrintoutLibrary.class);

        random.setDoubleVal(0);
        printoutFunction.run(channel, "Schooly_D", "schoolyd", "TSDBot I need a printout of a big ol' fuzzy bear");
        String lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("not computing"));

        for(int i=0 ; i < 3 ; i++) {
            printoutFunction.run(channel, "Schooly_D", "schoolyd", "nope :^)");
        }

        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("insolence"));

        printoutFunction.run(channel, "Schooly_D", "schoolyd", "TSDBot I need a printout of a big ol' fuzzy bear");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("make me"));

        printoutFunction.run(channel, "OpUser", "opuser", ".printout clear");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("the printout blacklist"));

        random.setDoubleVal(1.0);
        printoutFunction.run(channel, "Schooly_D", "schoolyd", "TSDBot I need a printout of a big ol' fuzzy bear");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.startsWith(server));
        String id = lastMessage.substring(lastMessage.lastIndexOf("/") + 1);
        byte[] printout = library.getPrintout(id);
        assertNotNull(printout);
        assertTrue(printout.length > 0);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            random = new FauxRandom();
            bind(Random.class).toInstance(random);
            bind(PrintoutLibrary.class).toInstance(new PrintoutLibrary());
            bind(String.class).annotatedWith(Names.named("serverUrl")).toInstance(server);

            TestBot testBot = new TestBot();
            testBot.addChannelUser(channel, User.Priv.OP, "OpUser");
            bind(Bot.class).toInstance(testBot);
        }
    }

    private static class FauxRandom extends Random {
        private double doubleVal;

        @Override
        public double nextDouble() {
            return doubleVal;
        }

        public void setDoubleVal(double doubleVal) {
            this.doubleVal = doubleVal;
        }
    }
}
