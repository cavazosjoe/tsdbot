package org.tsd.tsdbot.functions;

import com.google.inject.name.Names;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jibble.pircbot.User;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import static org.junit.Assert.*;
import static org.tsd.tsdbot.IntegTestUtils.sendMessageGetResponse;

@RunWith(JukitoRunner.class)
public class FilenameTest {

    private static final File library = new File(System.getProperty("java.io.tmpdir")+"/filenames");
    private static final String channel = "#tsd";
    private static final String serverUrl = IntegTestUtils.SERVER_URL;
    private static final String normalUserNick = "NormalUser";
    private static final String opUserNick = "OpUser";

    @Test
    public void testBlank(TSDBot bot) {
        TestBot2 testBot = (TestBot2)bot;
        String response = sendFromNormalGuy(testBot, ".fname");
        assertTrue(StringUtils.isNotBlank(response));
        assertTrue(response.startsWith(serverUrl));
    }

    @Test
    public void testQuery(TSDBot bot) {
        TestBot2 testBot = (TestBot2) bot;
        String response = sendFromNormalGuy(testBot, ".fname get red");
        assertTrue(response.equals(serverUrl + "/filenames/red_panda.png"));
        response = sendFromNormalGuy(testBot, ".fname get blergh");
        assertTrue(response.contains("found no filenames matching \"blergh\""));
    }

    @Test
    public void testNoPermissions(TSDBot bot) {
        TestBot2 testBot = (TestBot2) bot;

        String response = sendFromNormalGuy(testBot, ".fname add some_name.jpg http://imgur.com/something.jpg");
        assertTrue(response.startsWith("Only ops can add filenames directly"));

        response = sendFromNormalGuy(testBot, ".fname approve 1235");
        assertEquals("Only ops can approve filenames", response);

        response = sendFromNormalGuy(testBot, ".fname deny 1235");
        assertEquals("Only ops can deny filenames", response);

        assertEquals(2, library.listFiles().length);
    }

    @Test
    public void testAdd(TSDBot bot) {
        TestBot2 testBot = (TestBot2) bot;
        String response = sendFromOpGuy(testBot, ".fname add a_bear.gifv http://i.imgur.com/9lCE4Cv.gifv");
        assertEquals("Filename successfully added: "+serverUrl+"/filenames/a_bear.gifv", response);
        File f = new File(library, "a_bear.gifv");
        assertTrue(f.exists());
    }

    @Test
    public void testInferType(TSDBot bot) {
        TestBot2 testBot = (TestBot2) bot;
        String response = sendFromOpGuy(testBot, ".fname add a_bear http://i.imgur.com/9lCE4Cv.gifv");
        assertEquals("Filename successfully added: "+serverUrl+"/filenames/a_bear.gifv", response);
        File f = new File(library, "a_bear.gifv");
        assertTrue(f.exists());
    }

    @Test
    public void testSubmission(TSDBot bot, FilenameLibrary filenameLibrary) {
        TestBot2 testBot = (TestBot2) bot;

        String response = sendFromNormalGuy(testBot, ".fname submit a_bear.gifv http://i.imgur.com/9lCE4Cv.gifv");
        assertEquals(1, filenameLibrary.listSubmissions().size());
        FilenameLibrary.FilenameSubmission submission = filenameLibrary.listSubmissions().get(0);
        assertEquals("Submitted filename for approval (id = " + submission.getId() + ")", response);
        assertEquals("a_bear.gifv", submission.getName());
        assertEquals("http://i.imgur.com/9lCE4Cv.gifv", submission.getUrl());
        assertEquals("NormalUser", submission.getSubmitterNick());
    }

    @Test
    public void testTooManySubmissions(TSDBot bot) {
        TestBot2 testBot = (TestBot2) bot;

        String[] urls = {
                "http://i.imgur.com/Fflfv5o.png",
                "http://i.imgur.com/PaR95Sl.png",
                "http://i.imgur.com/X6C1qUf.png",
                "http://i.imgur.com/6Be0MzY.png",
                "http://i.imgur.com/dKwxHS8.png",
                "http://i.imgur.com/kP9mhsN.png"
        };

        String response = null;
        for(String url : urls) {
            String rand = RandomStringUtils.randomAlphabetic(10);
            String msg = String.format(".fname submit %s %s", rand, url);
            response = sendFromNormalGuy(testBot, msg);
        }

        assertEquals("Error: you currently have 5 submissions pending. Ask an op to review them", response);
    }

    @Test
    public void testSubmitAndApprove(TSDBot bot, FilenameLibrary filenameLibrary) {
        TestBot2 testBot = (TestBot2) bot;

        String response = sendFromNormalGuy(testBot, ".fname submit a_pic http://i.imgur.com/Fflfv5o.png");
        String id = filenameLibrary.listSubmissions().get(0).getId();
        assertEquals("Submitted filename for approval (id = " + id + ")", response);

        response = sendFromOpGuy(testBot, ".fname approve " + id);
        assertEquals("Filename approved: "+serverUrl+"/filenames/a_pic.png", response);
        assertEquals(0, filenameLibrary.listSubmissions().size());

        File f = new File(library+"/a_pic.png");
        assertTrue(f.exists());
    }

    @Test
    public void testSubmitAndDeny(TSDBot bot, FilenameLibrary filenameLibrary) {
        TestBot2 testBot = (TestBot2) bot;

        String response = sendFromNormalGuy(testBot, ".fname submit a_pic http://i.imgur.com/Fflfv5o.png");
        String id = filenameLibrary.listSubmissions().get(0).getId();
        assertEquals("Submitted filename for approval (id = " + id + ")", response);

        response = sendFromOpGuy(testBot, ".fname deny " + id);
        assertTrue(response.contains("DENIED"));
        assertEquals(0, filenameLibrary.listSubmissions().size());

        File f = new File(library+"/a_pic.png");
        assertFalse(f.exists());
    }

    @Test
    public void testInvalidUrl(TSDBot bot) {
        TestBot2 testBot = (TestBot2) bot;

        String[] badUrls = {
                "..::something.gifv",
                "::something.gifv",
                "//something.gifv"
        };

        for(String url : badUrls) {
            String response = sendFromOpGuy(testBot, ".fname add a_bear.gifv " + url);
            assertEquals("Error: not a valid URL", response);
        }
    }

    @Test
    public void testMismatchedTypes(TSDBot bot) {
        TestBot2 testBot = (TestBot2) bot;
        String response = sendFromOpGuy(testBot, ".fname add a_bear.gifv http://www.blah.com/something.jpg");
        assertEquals("Error: URL must match filetype of name: gifv", response);
    }

    @Test
    public void testDuplicateName(TSDBot bot) {
        TestBot2 testBot = (TestBot2) bot;
        String response = sendFromOpGuy(testBot, ".fname add red_panda.png http://www.blah.com/something.png");
        assertEquals("Error: detected an existing filename named red_panda.png", response);
    }



    @Before
    public void setup() throws Exception {
        String[] seedFilenames = {"cooking_with_schooly.jpg", "red_panda.png"};
        for(String s : seedFilenames) {
            IOUtils.copy(
                    getClass().getResourceAsStream("/filenames/"+s),
                    new FileOutputStream(new File(library, s))
            );
        }
    }

    @After
    public void cleanup(FilenameLibrary filenameLibrary) {
        filenameLibrary.clearSubmissions();
        for(File f : library.listFiles()) {
            f.delete();
        }
    }

    private String sendFromNormalGuy(TestBot2 bot, String message) {
        return sendMessageGetResponse(bot, normalUserNick, "normal", channel, message);
    }

    private String sendFromOpGuy(TestBot2 bot, String message) {
        return sendMessageGetResponse(bot, opUserNick, "important", channel, message);
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            if(!library.exists()) {
                library.mkdir();
            }
            install(new TestBotModule(channel));
            bind(File.class).annotatedWith(Names.named("filenameLibrary"))
                    .toInstance(library);
            bind(Random.class).toInstance(new Random());
            bind(FilenameLibrary.class);

            TestBot2 testBot = new TestBot2();
            testBot.addUser(IntegTestUtils.createUserWithPriv(opUserNick, User.Priv.OP), channel);
            testBot.addUser(IntegTestUtils.createUserWithPriv(normalUserNick, User.Priv.NONE), channel);
            bind(TSDBot.class).toInstance(testBot);

            IntegTestUtils.loadFunctions(binder(), Filename.class);

            requestInjection(testBot);
        }
    }
}
