package org.tsd.tsdbot.warzone.library;

import com.google.inject.Inject;

import java.util.Random;

public class LibraryUtils {

    private final Random random;

    @Inject
    public LibraryUtils(Random random) {
        this.random = random;
    }

    public String plural(int amount, String singular, String plural) {
        return amount + " " + ( amount == 1 ? singular : plural );
    }
}
