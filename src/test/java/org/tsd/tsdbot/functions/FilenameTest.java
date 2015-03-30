package org.tsd.tsdbot.functions;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.Bot;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TestBot;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by Joe on 3/29/2015.
 */
@RunWith(JukitoRunner.class)
public class FilenameTest {

    private static final String channel = "#tsd";
    private static final String filenamesLocation = "http://www.teamschoolyd.org/filenames/";

    @Test
    public void test(Bot bot, HttpClient client) throws IOException {
        TestBot testBot = (TestBot)bot;

        bot.onMessage(channel, "Schooly_D", "schoolyd", "hostname", ".fname");
        String lastMessage = testBot.getLastMessage(channel);
        assertTrue(lastMessage.contains(filenamesLocation));

        HttpGet get = new HttpGet(lastMessage);
        HttpResponse response = client.execute(get);
        assertEquals(200, response.getStatusLine().getStatusCode());
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            bind(Random.class).toInstance(new Random());
            bind(HttpClient.class).toInstance(HttpClients.createMinimal());
            TestBot testBot = new TestBot(channel, null);
            bind(Bot.class).toInstance(testBot);
            IntegTestUtils.loadFunctions(binder(), Filename.class);
            requestInjection(testBot);
        }
    }
}
