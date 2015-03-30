package org.tsd.tsdbot.logic;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tsd.tsdbot.util.TSDTVUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by Joe on 3/25/2015.
 */
public class TSDTVUtilTest {

    private static File tsdtvFolder;
    private static HashSet<File> createdFiles = new HashSet<>();

    @BeforeClass
    public static void setupFileSystem() throws IOException {
        String property = "java.io.tmpdir";
        String tempDirPath = System.getProperty(property);
        String tsdtvPath = tempDirPath + "/tsdtv";
        tsdtvFolder = new File(tsdtvPath);
        tsdtvFolder.mkdir();

        File newFile;
        for(int i=0 ; i < 5 ; i++) {
            newFile = new File(tsdtvPath + "/" + RandomStringUtils.randomAlphabetic(10) + ".tmp");
            newFile.createNewFile();
            createdFiles.add(newFile);
        }
    }

    @Test
    public void testGetEpisodeNumberFromFilename() {

        HashMap<String, Integer> episodes = new HashMap<>();
        episodes.put("1_Someone_dies.mkv", 1);
        episodes.put("01_Someone_dies.mkv", 1);
        episodes.put("01--Someone_dies.mkv", 1);
        episodes.put("1Someone_dies.mkv", 1);
        episodes.put("0001_Someone_dies.mkv", 1);
        episodes.put("12_Someone_dies.mkv", 12);
        episodes.put("1.mkv", 1);
        episodes.put("1___.mkv", 1);
        episodes.put("10_Someone_dies.mkv", 10);
        episodes.put("1_-_Someone_dies.mkv", 1);
        episodes.put("1.0-Someone_dies.mkv", 1);

        for(String ep : episodes.keySet()) try {
            assertEquals((int)episodes.get(ep), TSDTVUtil.getEpisodeNumberFromFilename(ep));
        } catch (Exception e) {fail();}

    }

    @Test
    public void testGetFileFromDirectory() {

        assertNotNull(tsdtvFolder);
        assertTrue(tsdtvFolder.isDirectory());

        File randomFile;
        Random random = new Random();
        for(int i=0 ; i < 100 ; i++) {
            randomFile = TSDTVUtil.getRandomFileFromDirectory(random, tsdtvFolder);
            assertNotNull(randomFile);
            assertTrue(randomFile.isFile());
            assertTrue(createdFiles.contains(randomFile));
        }

    }

    @AfterClass
    public static void cleanFileSystem() throws IOException {
        FileUtils.deleteDirectory(tsdtvFolder);
    }
}
