package org.tsd.tsdbot.warzone;

import java.util.*;

public class Library {

    public final FillableQueue<String> teamNames = new FillableQueue<>(Arrays.asList(
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

    public final FillableQueue<StringGenerator> introSentences = new FillableQueue<>(Arrays.asList(
            StringGenerator.instance(new StringGenStrategy<IntroParams>() {
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

                    builder.append(plural(params.totalWins, "win", "wins"))
                            .append(" over ")
                            .append(plural(params.totalGames(), "game", "games"))
                            .append(".");

                    return builder.toString();
                }
            }),
            StringGenerator.instance(new StringGenStrategy<IntroParams>() {
                @Override
                public String gen(StringBuilder builder, IntroParams params) {
                    builder.append(params.teamName).append(" had an absolutely ");
                    if(params.winPct() < .4)
                        builder.append("abysmal");
                    else if(params.winPct() > .8)
                        builder.append("fantastic");
                    else
                        builder.append("okay");

                    builder.append(" time in Halo 5's \"Warzone\" playlist, playing ")
                            .append(plural(params.totalGames(), "game", "games"));

                    if(params.winPct() < .4)
                        builder.append(" but limping away with a disappointing");
                    else {
                        builder.append(" and walking away with ");
                        if(params.winPct() > .8)
                            builder.append("and exceptional");
                        else
                            builder.append("a solid");
                    }

                    builder.append(" ").append(plural(params.totalWins, "win", "wins")).append(".");

                    return builder.toString();
                }
            })
    ));

    public static class FillableQueue<T> {

        private List<T> seeds;
        private LinkedList<T> queue = new LinkedList<>();

        public FillableQueue(Collection<T> items) {
            this.seeds = new LinkedList<>(items);
        }

        public T pop() {
            if(queue.isEmpty()) {
                queue.addAll(seeds);
                Collections.shuffle(queue);
            }
            return queue.pop();
        }

    }

    public static class StringGenerator<T extends GeneratorParams> {
        private final StringGenStrategy strategy;
        protected final StringBuilder builder = new StringBuilder();
        private StringGenerator(StringGenStrategy strategy) {
            this.strategy = strategy;
        }
        static StringGenerator instance(StringGenStrategy strategy) {
            return new StringGenerator(strategy);
        }
        public final String gen(T params) {
            return strategy.gen(builder, params);
        }
    }

    static interface StringGenStrategy<T extends GeneratorParams> {
        String gen(StringBuilder builder, T params);
    }

    static interface GeneratorParams {}

    static class IntroParams implements GeneratorParams {

        public static final String TOTAL_WINS = "totalWins";
        public static final String TOTAL_LOSSES = "totalLosses";
        public static final String TEAM_NAME = "teamName";

        public final int totalWins;
        public final int totalLosses;
        public final String teamName;

        public IntroParams(Map<String, Object> params) {
            this.totalWins = (int) params.get(TOTAL_WINS);
            this.totalLosses = (int) params.get(TOTAL_LOSSES);
            this.teamName = (String) params.get(TEAM_NAME);
        }

        public int totalGames() {
            return totalWins + totalLosses;
        }

        public double winPct() {
            return (double)totalWins / (double)(totalGames());
        }
    }

    private static String plural(int amount, String singular, String plural) {
        return amount + " " + ( amount == 1 ? singular : plural );
    }

}
