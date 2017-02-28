package org.tsd.tsdbot.util.fuzzy;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FuzzyLogic {

    public static boolean fuzzyMatches(String query, String text) {
        return query == null && text == null
                || !(query == null || text == null)
                && text.toLowerCase().contains(query.toLowerCase());
    }

    public static <T> List<T> fuzzySubset(String query, Collection<T> choices, Function<T, String> toString) {
        return choices.stream()
                .filter(choice -> fuzzyMatches(query, toString.apply(choice)))
                .collect(Collectors.toList());
    }

}
