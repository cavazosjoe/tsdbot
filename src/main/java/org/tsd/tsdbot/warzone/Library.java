package org.tsd.tsdbot.warzone;

import java.util.*;

public class Library {

    public final FillableQueue teamNames = new FillableQueue(
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
    );

    public final MappedQueue introSentences = new MappedQueue(new Object[][]{
            {Mood.terrible, "It was a night to forget for %(teamName) as they notched only %(totalWinsPretty) over " +
                    "%(totalGamesPretty)."},
            {Mood.terrible, "%(teamName) had an absolutely abysmal time in Halo 5's \"Warzone\" playlist, playing " +
                    "%(totalGamesPretty) but scoring only %(totalWinsPretty) against %(totalLossesPretty)."},
            {Mood.bad, "Thing could have gone better for %(teamName) as they went into Warzone, playing " +
                    "%(totalGamesPretty) but limping away with a disappointing %(totalWinsPretty)."},
            {Mood.bad, "Not the best night for %(teamName) as they played %(totalGames) games but scored only " +
                    "%(totalWins) wins."}

    });

    public static class FillableQueue {

        private List<String> seeds;
        private LinkedList<String> queue = new LinkedList<>();

        public FillableQueue(String... items) {
            this.seeds = Arrays.asList(items);
        }

        public FillableQueue(Collection<String> items) {
            this.seeds = new LinkedList<>(items);
        }

        public String pop() {
            if(queue.isEmpty()) {
                queue.addAll(seeds);
                Collections.shuffle(queue);
            }
            return queue.pop();
        }

    }

    public static class MappedQueue {

        private HashMap<Object, List<String>> seeds;
        private HashMap<Object, FillableQueue> queueMap = new HashMap<>();

        public MappedQueue(Object[]... items) {
            seeds = new HashMap<>();
            for(Object[] item : items) {
                Object key = item[0];
                if(!seeds.containsKey(key)) {
                    seeds.put(key, new LinkedList<String>());
                }
                seeds.get(key).add((String) item[1]);
            }
        }

        public String pop(Object key) {
            if(!queueMap.containsKey(key)) {
                queueMap.put(key, new FillableQueue(seeds.get(key)));
            }
            return queueMap.get(key).pop();
        }

    }

    public enum Mood {
        terrible,
        bad,
        normal,
        good,
        great
    }

}
