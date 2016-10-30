package org.tsd.tsdbot.markov;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class MarkovFileManagerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private MarkovFileManager markovFileManager;

    @Before
    public void setup() {
        markovFileManager = new MarkovFileManager(temporaryFolder.getRoot());
    }

    @Test
    public void testAddToFile() throws Exception {
        String filename = "Cody Miller.txt";
        markovFileManager.addToFile(filename, new MarkovKey("hey", "you"), "guy");

        File markovFile = new File(temporaryFolder.getRoot(), filename);
        assertTrue(markovFile.exists());
        assertEquals("[hey you]guy", FileUtils.readFileToString(markovFile, Charset.defaultCharset()));
    }

    @Test
    public void testAddMultipleToFile() throws Exception {
        String filename = "Znite.txt";
        markovFileManager.addToFile(filename, new MarkovKey("hey", "you"), "guy");
        markovFileManager.addToFile(filename, new MarkovKey("hey", "you"), "dude");

        File markovFile = new File(temporaryFolder.getRoot(), filename);
        assertTrue(markovFile.exists());
        assertEquals("[hey you]guy,dude", FileUtils.readFileToString(markovFile, Charset.defaultCharset()).trim());
    }

    @Test
    public void testAddManyToFile() throws Exception {
        String filename = "Revenant.txt";
        int valueCount = 1000;
        String key1;
        String key2;
        String value;
        for(int i=0 ; i < valueCount ; i++) {
            key1 = RandomStringUtils.randomAlphabetic(1);
            key2 = RandomStringUtils.randomAlphabetic(1);
            value = RandomStringUtils.randomAlphabetic(10);
            markovFileManager.addToFile(filename, new MarkovKey(key1, key2), value);
        }
    }

    @Test
    public void testGetWord() throws Exception {
        String filename = "Dorj.txt";
        MarkovKey key = new MarkovKey("one", "two");
        markovFileManager.addToFile(filename, key, "engage");
        String[] result = markovFileManager.getWordsForKey(filename, key);
        assertArrayEquals(new String[]{"engage"}, result);
    }

    @Test
    public void testGetWords() throws Exception {
        String filename = "Double Dorj.txt";
        MarkovKey key = new MarkovKey("one", "two");
        markovFileManager.addToFile(filename, key, "engage");
        markovFileManager.addToFile(filename, key, "justice");
        String[] result = markovFileManager.getWordsForKey(filename, key);
        assertArrayEquals(new String[]{"engage", "justice"}, result);
    }
}
