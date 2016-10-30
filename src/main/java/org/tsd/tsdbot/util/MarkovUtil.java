package org.tsd.tsdbot.util;

public class MarkovUtil {

    public static String sanitize(String input) {
        return input != null ? input.replaceAll("[^\\w]", "").toLowerCase().trim() : null;
    }
}
