package org.tsd.tsdbot.util;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * Created by Joe on 9/3/2014.
 */
public class MiscUtils {

    public static final String URL_REGEX = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    public static String getRandomString() {
        return getRandomString(10);
    }

    public static String getRandomString(int numChars) {
        return RandomStringUtils.randomAlphanumeric(numChars);
    }
}
