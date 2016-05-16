package org.tsd.tsdbot;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import org.jibble.pircbot.User;
import org.tsd.tsdbot.functions.MainFunction;

import java.io.InputStream;
import java.util.Properties;

public class IntegTestUtils {

    public static final String MAIN_CHANNEL = "#tsd";
    public static final String BOT_OWNER = "BotOwner";
    public static final String SERVER_URL = "http://irc.org";

    @SafeVarargs
    public static void loadFunctions(Binder binder, Class<? extends MainFunction>... functions) {
        Multibinder<MainFunction> functionBinder = Multibinder.newSetBinder(binder, MainFunction.class);
        for(Class<? extends MainFunction> function : functions) {
            functionBinder.addBinding().to(function);
        }
    }

    public static String loadProperty(String key) {
        try(InputStream is = IntegTestUtils.class.getResourceAsStream("/test.properties")) {
            Properties properties = new Properties();
            properties.load(is);
            return properties.getProperty(key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load properties file");
        }
    }

    public static String sendMessageGetResponse(TestBot2 testBot, String user, String ident, String channel, String msg) {
        testBot.onMessage(channel, user, ident, "hostname", msg);
        return testBot.getLastMessage(channel);
    }

    public static User createUserWithPriv(String handle, User.Priv priv) {
        return new User(priv.getPrefix(), handle);
    }
}
