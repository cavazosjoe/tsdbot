package org.tsd.tsdbot.warzone.library;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@SuppressWarnings("unchecked")
public class IntroLibrary {

    private final FillableQueue<StringGenStrategy> introSentences = new FillableQueue<>();

    @Inject
    public IntroLibrary(final LibraryUtils libraryUtils) {

        introSentences.add(new StringGenStrategy<IntroParams>() {
            @Override
            public String gen(StringBuilder builder, IntroParams params) {
                builder.append("It was a ");
                if (params.winPct() < .4)
                    builder.append("night to forget");
                else if (params.winPct() > .8)
                    builder.append("night to remember");
                else
                    builder.append("so-so night");

                builder.append(" for ").append(params.teamName).append(" as they notched ");

                if (params.winPct() < .5)
                    builder.append("only ");
                else if (params.winPct() > .8)
                    builder.append("an impressive ");

                builder.append(libraryUtils.plural(params.totalWins, "win", "wins"))
                        .append(" over ")
                        .append(libraryUtils.plural(params.totalGames(), "game", "games"))
                        .append(".");

                return builder.toString();
            }
        });

        introSentences.add(new StringGenStrategy<IntroParams>() {
            @Override
            public String gen(StringBuilder builder, IntroParams params) {
                builder.append(params.teamName).append(" had an absolutely ");
                if (params.winPct() < .4)
                    builder.append("abysmal");
                else if (params.winPct() > .8)
                    builder.append("fantastic");
                else
                    builder.append("okay");

                builder.append(" time in Halo 5's \"Warzone\" playlist, playing ")
                        .append(libraryUtils.plural(params.totalGames(), "game", "games"));

                if (params.winPct() < .4)
                    builder.append(" but limping away with a disappointing");
                else {
                    builder.append(" and walking away with ");
                    if (params.winPct() > .8)
                        builder.append("and exceptional");
                    else
                        builder.append("a solid");
                }

                builder.append(" ").append(libraryUtils.plural(params.totalWins, "win", "wins")).append(".");

                return builder.toString();
            }
        });
    }

    public String getIntroSentence(IntroParams introParams) {
        return introSentences.pop().gen(new StringBuilder(), introParams);
    }

}
