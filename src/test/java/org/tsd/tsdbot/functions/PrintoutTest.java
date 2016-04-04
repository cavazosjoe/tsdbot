package org.tsd.tsdbot.functions;

import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jibble.pircbot.User;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.*;
import org.tsd.tsdbot.config.GoogleConfig;

import java.io.FileNotFoundException;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JukitoRunner.class)
public class PrintoutTest {

    private static final String server = "http://test";
    private static final String channel = "#tsd";

    private static FauxRandom random;

    @Test
    public void testNormal(Injector injector) throws FileNotFoundException {
        TestBot bot = (TestBot) injector.getInstance(Bot.class);
        PrintoutLibrary library = injector.getInstance(PrintoutLibrary.class);

        random.setDoubleVal(1.0);
        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", "TSDBot I need a printout of a big ol' fuzzy bear");
        String lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.startsWith(server));
        String id = lastMessage.substring(lastMessage.lastIndexOf("/") + 1);
        assertTrue(id.endsWith(".jpg"));
        id = id.substring(0, id.indexOf(".jpg"));
        byte[] printout = library.getPrintout(id);
        assertNotNull(printout);
        assertTrue(printout.length > 0);
    }

    @Test
    public void testRepeat(Injector injector) throws FileNotFoundException {

        TestBot bot = (TestBot) injector.getInstance(Bot.class);
        PrintoutLibrary library = injector.getInstance(PrintoutLibrary.class);

        random.setDoubleVal(0);
        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", "TSDBot I need a printout of a big ol' fuzzy bear");
        String lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("not computing"));
        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", "BIG FUZZY BEAR");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("not computing"));

        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", "BIG. FUZZY. BEAR.");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.startsWith(server));
        String id = lastMessage.substring(lastMessage.lastIndexOf("/") + 1, lastMessage.indexOf(".jpg"));
        byte[] printout = library.getPrintout(id);
        assertNotNull(printout);
        assertTrue(printout.length > 0);
    }

    @Test
    public void testBlacklist(Injector injector) throws FileNotFoundException {

        TestBot bot = (TestBot) injector.getInstance(Bot.class);
        PrintoutLibrary library = injector.getInstance(PrintoutLibrary.class);

        random.setDoubleVal(0);
        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", "TSDBot I need a printout of a big ol' fuzzy bear");
        String lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("not computing"));

        for(int i=0 ; i < 3 ; i++) {
            bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", "nope :^)");
        }

        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("insolence"));

        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", "TSDBot I need a printout of a big ol' fuzzy bear");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("make me"));

        bot.onMessage(channel, "OpUser", "opuser", "hostname", ".printout clear");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("the printout blacklist"));

        random.setDoubleVal(1.0);
        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", "TSDBot I need a printout of a big ol' fuzzy bear");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.startsWith(server));
        String id = lastMessage.substring(lastMessage.lastIndexOf("/") + 1, lastMessage.indexOf(".jpg"));
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
            bind(HttpClient.class).toInstance(HttpClients.createMinimal());

            TestBot testBot = new TestBot(channel, null);
            testBot.addChannelUser(channel, User.Priv.OP, "OpUser");
            bind(Bot.class).toInstance(testBot);

            GoogleConfig googleConfig = new GoogleConfig();
            googleConfig.gisCx = IntegTestUtils.loadProperty("gisCx");
            googleConfig.apiKey = IntegTestUtils.loadProperty("apiKey");
            bind(GoogleConfig.class).toInstance(googleConfig);

            IntegTestUtils.loadFunctions(binder(), Printout.class);

            requestInjection(testBot);
        }
    }
}
