package org.tsd.tsdbot.util;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class TSDFMUtil {

    public static String getSanitizedSongName(File songFile) {

        // replace all underscores and dashes with spaces
        // recombine the string so words are separated by one space
        String cleanedName = songFile.getName()
                .replaceAll("-", " ")
                .replaceAll("_", " ");
        String[] parts = cleanedName.split("\\s+");
        cleanedName = StringUtils.join(parts, " ");

        // strip off the extension
        cleanedName = cleanedName.substring(0, cleanedName.lastIndexOf('.'));

        // trim
        cleanedName = cleanedName.trim();

        return cleanedName;
    }
}
