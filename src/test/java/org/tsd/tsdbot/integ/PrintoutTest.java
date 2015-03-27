package org.tsd.tsdbot.integ;

import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.PrintoutLibrary;
import org.tsd.tsdbot.TSDBot;
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

    private static String server = "http://test";
    private static String channel = "#tsd";

    @Test
    public void test(Injector injector) throws FileNotFoundException {
        Printout printoutFunction = injector.getInstance(Printout.class);
        TestBot bot = (TestBot) injector.getInstance(Bot.class);
        PrintoutLibrary library = injector.getInstance(PrintoutLibrary.class);

        printoutFunction.run(channel, "Schooly_D", "schoolyd", "TSDBot I need a printout of a big ol' fuzzy bear");
        assertEquals(1, bot.getAllMessages(channel).size());

        String lastMessage = bot.getLastMessage(channel);
        if(lastMessage.toLowerCase().startsWith("not computing")) {
            printoutFunction.run(channel, "Schooly_D", "schoolyd", "BIG. FUZZY. BEAR.");
            assertEquals(2, bot.getAllMessages(channel).size());
            lastMessage = bot.getLastMessage(channel);
        }

        assertTrue(lastMessage.startsWith(server));
        String id = lastMessage.substring(lastMessage.lastIndexOf("/") + 1);
        byte[] printout = library.getPrintout(id);
        assertNotNull(printout);
        assertTrue(printout.length > 0);

    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            bind(Random.class).toInstance(new Random());
            bind(PrintoutLibrary.class).toInstance(new PrintoutLibrary());
            bind(String.class).annotatedWith(Names.named("serverUrl")).toInstance(server);
            bind(Bot.class).toInstance(new TestBot());
        }
    }
}
