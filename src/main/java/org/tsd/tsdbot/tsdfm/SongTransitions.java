package org.tsd.tsdbot.tsdfm;

import java.util.Random;

public class SongTransitions {

    private static final String[] formats = {
            "You're tuned into TSDFM. Coming up next, %s with %s",
            "You've got it locked into TSDFM, 77 point 7 WTSD. Up next, %s with their new hit single %s",
            "There are those who said this day would never come. What are they to say now?",
            "Don't touch that dial, the hits keep on coming with TSDFM. Next up, %s with %s",
            "TSDFM. The official radio station of red pandas everywhere. Next up, %s with %s"
    };

    public static String get(String artist, String song) {
        Random random = new Random();
        return String.format(formats[random.nextInt(formats.length)], artist, song);
    }
}
