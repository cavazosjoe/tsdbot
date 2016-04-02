package org.tsd.tsdbot.warzone.library;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Arrays;

@Singleton
public class Library {

    private final LibraryUtils libraryUtils;
    private final IntroLibrary introLibrary;

    @Inject
    public Library(LibraryUtils libraryUtils, IntroLibrary introLibrary) {
        this.libraryUtils = libraryUtils;
        this.introLibrary = introLibrary;
    }

    public String getTeamName() {
        return teamNames.pop();
    }

    public IntroLibrary getIntroLibrary() {
        return introLibrary;
    }

    private final FillableQueue<String> teamNames = new FillableQueue<>(Arrays.asList(
            "%s and the Funky Bunch",
            "%s and the Way Outs",
            "Team %s",
            "The Original Team %s",
            "%s and The Crew",
            "%s and The Cleaners",
            "%s and Friends",
            "%s and Company",
            "%s and the Motown Assassins",
            "%s and the Lady Panthers"
    ));

}
