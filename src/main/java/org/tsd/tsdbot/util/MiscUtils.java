package org.tsd.tsdbot.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.security.MessageDigest;
import java.util.Formatter;

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

    public static String sha1(String input) {
        String sha1 = null;
        try(Formatter formatter = new Formatter()) {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(input.getBytes("UTF-8"));
            for (byte b : crypt.digest()) {
                formatter.format("%02x", b);
            }
            sha1 = formatter.toString();
        } catch (Exception e) {
            // eh
        }
        return sha1;
    }
}
