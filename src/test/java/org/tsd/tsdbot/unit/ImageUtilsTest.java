package org.tsd.tsdbot.unit;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.tsd.tsdbot.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.nio.Buffer;

import static org.junit.Assert.*;

/**
 * Created by Joe on 3/26/2015.
 */
@RunWith(Theories.class)
public class ImageUtilsTest {

    private static BufferedImage bgImg = null;
    private static BufferedImage fgImg = null;
    private static BufferedImage result = null;

    @DataPoints
    public static int[] sizes() {
        return new int[]{
                1, 2, 3, 4, 5, 10, 100, 1000 // don't get too big now -- heap space concerns
        };
    }

    @Theory
    public void test(Integer bgWidth, Integer bgHeight, Integer fgWidth, Integer fgHeight) {

        Assume.assumeTrue(bgHeight > 0 && bgWidth > 0 && fgHeight > 0 && fgWidth > 0);

        bgImg = new BufferedImage(bgWidth, bgHeight, BufferedImage.TYPE_BYTE_GRAY);
        fgImg = new BufferedImage(fgWidth, fgHeight, BufferedImage.TYPE_BYTE_GRAY);

        result = ImageUtils.overlayImages(bgImg, fgImg);
        assertNotNull(result);
        assertEquals(result.getHeight(), (int)bgHeight);
        assertEquals(result.getWidth(), (int)bgWidth);
    }
}
