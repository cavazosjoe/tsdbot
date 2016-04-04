package org.tsd.tsdbot;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import org.tsd.tsdbot.functions.MainFunction;

import java.io.InputStream;
import java.util.Properties;

public class IntegTestUtils {

    @SafeVarargs
    public static void loadFunctions(Binder binder, Class<? extends MainFunction>... functions) {
        Multibinder<MainFunction> functionBinder = Multibinder.newSetBinder(binder, MainFunction.class);
        for(Class<? extends MainFunction> function : functions)
            functionBinder.addBinding().to(function);
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

    public static String sendMessageGetResponse(TestBot testBot, String user, String ident, String channel, String msg) {
        testBot.onMessage(channel, user, ident, "hostname", msg);
        return testBot.getLastMessage(channel);
    }
}
