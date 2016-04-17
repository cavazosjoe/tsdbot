package org.tsd.tsdbot.logic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.tsd.tsdbot.util.TSDFMUtil;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TSDFMUtilTest {

    @Test
    public void testGetSanitizedSongName() {

        String[][] data = new String[][]{
                {"hurr.mp3",                    "hurr"},
                {"hurr-durr.wav",               "hurr durr"},
                {"hold_my___beer.mp3",          "hold my beer"},
                {"---you there ___ ayy .ogg",   "you there ayy"}
        };

        for(String[] row : data) {
            File f = mock(File.class);
            when(f.getName()).thenReturn(row[0]);
            String cleanName = TSDFMUtil.getSanitizedSongName(f);
            assertEquals(row[1], cleanName);
        }

    }
}
