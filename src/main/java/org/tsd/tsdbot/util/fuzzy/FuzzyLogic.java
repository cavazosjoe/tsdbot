package org.tsd.tsdbot.util.fuzzy;

import java.util.LinkedList;

public class FuzzyLogic {

    public static boolean fuzzyMatches(String query, String text) {
        if(query == null && text == null) return true;
        else if(query == null || text == null) return false;
        else return text.toLowerCase().contains(query.toLowerCase());
    }

    public static <T> LinkedList<T> fuzzySubset(String query, Iterable<T> choices, FuzzyVisitor<T> visitor) {
        LinkedList<T> ret = new LinkedList<>();
        for(T choice : choices) {
            if(fuzzyMatches(query, visitor.visit(choice)))
                ret.addFirst(choice);
        }
        return ret;
    }

}
