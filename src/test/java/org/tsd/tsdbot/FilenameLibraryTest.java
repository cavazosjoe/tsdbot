package org.tsd.tsdbot;

import com.google.inject.name.Names;
import org.apache.http.client.HttpClient;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@RunWith(JukitoRunner.class)
public class FilenameLibraryTest {

    private static final String serverUrl = "http://tsd.org";

    @Test
    public void testNameCleanup(FilenameLibrary filenameLibrary) throws Exception {
        String[][] goodData = new String[][]{
                {"a_bear",              "http://bear.com/bear.jpg", "a_bear.jpg"},
                {"a_bear.png",          "http://bear.com/bear.png", "a_bear.png"},
                {"gv_joins_#tsd.jpg",   "http://bear.com/bear.jpg", "gv_joins_tsd.jpg"}
        };

        for(String[] row : goodData) {
            assertEquals(row[2], filenameLibrary.correctNameIfNecessary(row[0], row[1]));
        }

        String[][] badData = new String[][]{
                {"a_bear.png", "http://bear.com/bear.jpg"},
                {"a_bear.png", "http://bear.com/bear"}
        };

        for(String[] row: badData) try {
            filenameLibrary.correctNameIfNecessary(row[0], row[1]);
            fail();
        } catch (FilenameLibrary.FilenameValidationException e) {}
    }

    public static class Module extends JukitoModule {
        @Override
        protected void configureTest() {
            File mockLibrary = mock(File.class);
            bind(File.class).annotatedWith(Names.named("filenameLibrary"))
                    .toInstance(mockLibrary);

            HttpClient mockHttpClient = mock(HttpClient.class);
            bind(HttpClient.class).toInstance(mockHttpClient);

            bind(Random.class).toInstance(new Random());

            bind(String.class).annotatedWith(Names.named("serverUrl"))
                    .toInstance(serverUrl);
        }
    }

}
