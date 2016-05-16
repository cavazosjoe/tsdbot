package org.tsd.tsdbot.functions;

import com.maxsvett.fourchan.board.Board;
import org.jibble.pircbot.User;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.IntegTestUtils;
import org.tsd.tsdbot.TSDBot;
import org.tsd.tsdbot.TestBot2;
import org.tsd.tsdbot.TestBotModule;

import java.util.Random;

import static org.junit.Assert.assertTrue;

@RunWith(JukitoRunner.class)
public class FourChanTest {

    private static final String channel = "#tsd";
    private static final String normalUser = "Schooly_D";
    private static final String imagePattern = "http://images\\.4chan\\.org%ssrc/[\\d]+\\.[\\w]+";

    @Test
    @Ignore
    public void test(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;

        String lastMessage = sendMessageGetResponse(testBot, "normalUser", ".4ch");
        assertTrue(lastMessage.toLowerCase().startsWith("usage:"));

        lastMessage = sendMessageGetResponse(testBot, "normalUser", ".4ch v g");
        assertTrue(lastMessage.toLowerCase().startsWith("usage:"));

        for(String badBoardName : new String[]{"board", ":;;", "aboard!", "v!"}) {
            lastMessage = sendMessageGetResponse(testBot, "normalUser", ".4ch " + badBoardName);
            assertTrue(lastMessage.toLowerCase().startsWith("could not understand"));
        }

        for(Board board : Board.BOARDS) {
            lastMessage = sendMessageGetResponse(testBot, "normalUser", ".4ch " + board.getPath());
            if(board.isNsfw())
                assertTrue(lastMessage.toLowerCase().startsWith("i don't support"));
            else {
                String[] parts = lastMessage.split("\\s+");
                assertTrue(parts[parts.length - 1].matches(String.format(imagePattern, board.getPath())));

                // try the same board with slashes removed from query, e.g: .4ch /v/ -> .4ch v
                lastMessage = sendMessageGetResponse(testBot, "normalUser", ".4ch " + board.getPath().replaceAll("/",""));
                parts = lastMessage.split("\\s+");
                assertTrue(parts[parts.length-1].matches(String.format(imagePattern, board.getPath())));
            }
        }
    }

    private String sendMessageGetResponse(TestBot2 testBot, String user, String msg) {
        testBot.onMessage(channel, user, "ident", "hostname", msg);
        return testBot.getLastMessage(channel);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            install(new TestBotModule(channel));
            bind(Random.class).toInstance(new Random());
            TestBot2 testBot = new TestBot2();
            testBot.addUser(IntegTestUtils.createUserWithPriv(normalUser, User.Priv.NONE), channel);
            bind(TSDBot.class).toInstance(testBot);

            IntegTestUtils.loadFunctions(binder(), FourChan.class);

            requestInjection(testBot);
        }
    }
}
