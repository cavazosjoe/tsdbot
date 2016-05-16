package org.tsd.tsdbot.functions;

import com.google.inject.Injector;
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

    private static final String channel = IntegTestUtils.MAIN_CHANNEL;
    private static final String server = IntegTestUtils.SERVER_URL;
    private static final String normalUser = "NormalUser";
    private static final String opUser = "OpUser";

    private static FauxRandom random;

    @Test
    public void testNormal(Injector injector) throws FileNotFoundException {
        TestBot2 bot = (TestBot2) injector.getInstance(TSDBot.class);
        PrintoutLibrary library = injector.getInstance(PrintoutLibrary.class);

        random.setDoubleVal(1.0);
        sendMessage(bot, normalUser, "TSDBot I need a printout of a big ol' fuzzy bear");
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

        TestBot2 bot = (TestBot2) injector.getInstance(TSDBot.class);
        PrintoutLibrary library = injector.getInstance(PrintoutLibrary.class);

        random.setDoubleVal(0);
        sendMessage(bot, normalUser, "TSDBot I need a printout of a big ol' fuzzy bear");
        String lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("not computing"));
        sendMessage(bot, normalUser, "BIG FUZZY BEAR");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("not computing"));

        sendMessage(bot, normalUser, "BIG. FUZZY. BEAR.");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.startsWith(server));
        String id = lastMessage.substring(lastMessage.lastIndexOf("/") + 1, lastMessage.indexOf(".jpg"));
        byte[] printout = library.getPrintout(id);
        assertNotNull(printout);
        assertTrue(printout.length > 0);
    }

    @Test
    public void testBlacklist(Injector injector) throws FileNotFoundException {

        TestBot2 bot = (TestBot2) injector.getInstance(TSDBot.class);
        PrintoutLibrary library = injector.getInstance(PrintoutLibrary.class);

        random.setDoubleVal(0);
        sendMessage(bot, normalUser, "TSDBot I need a printout of a big ol' fuzzy bear");
        String lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("not computing"));

        for(int i=0 ; i < 3 ; i++) {
            sendMessage(bot, normalUser, "nope :^)");
        }

        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("insolence"));

        sendMessage(bot, normalUser, "TSDBot I need a printout of a big ol' fuzzy bear");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("make me"));

        sendMessage(bot, opUser, ".printout clear");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.toLowerCase().startsWith("the printout blacklist"));

        random.setDoubleVal(1.0);
        sendMessage(bot, normalUser, "TSDBot I need a printout of a big ol' fuzzy bear");
        lastMessage = bot.getLastMessage(channel);
        assertTrue(lastMessage.startsWith(server));
        String id = lastMessage.substring(lastMessage.lastIndexOf("/") + 1, lastMessage.indexOf(".jpg"));
        byte[] printout = library.getPrintout(id);
        assertNotNull(printout);
        assertTrue(printout.length > 0);
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
            bind(PrintoutLibrary.class).toInstance(new PrintoutLibrary());

            TestBot2 testBot = new TestBot2();
            testBot.addUser(IntegTestUtils.createUserWithPriv(normalUser, User.Priv.NONE), channel);
            testBot.addUser(IntegTestUtils.createUserWithPriv(opUser, User.Priv.OP), channel);
            bind(TSDBot.class).toInstance(testBot);

            GoogleConfig googleConfig = new GoogleConfig();
            googleConfig.gisCx = IntegTestUtils.loadProperty("gisCx");
            googleConfig.apiKey = IntegTestUtils.loadProperty("apiKey");
            bind(GoogleConfig.class).toInstance(googleConfig);

            IntegTestUtils.loadFunctions(binder(), Printout.class);

            requestInjection(testBot);
        }
    }
}
