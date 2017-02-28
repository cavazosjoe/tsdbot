package org.tsd.tsdbot.util;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtils {

    public static BufferedImage overlayImages(BufferedImage bgImage,
                                              BufferedImage fgImage) {
        Graphics2D g = bgImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(bgImage, 0, 0, null);
        g.drawImage(fgImage, 0, 0, null);
        g.dispose();
        return bgImage;
    }
}
