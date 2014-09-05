package org.tsd.tsdbot.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.LinkedList;

/**
 * Created by Joe on 9/3/2014.
 */
public class FuzzyLogic {

    public static boolean fuzzyMatches(String query, String element) {
        if(query == null && element == null) return true;
        else if(query == null || element == null) return false;
        else return element.toLowerCase().contains(query.toLowerCase());
    }

    public static <T> LinkedList<T> fuzzySubset(String query, Iterable<T> choices, FuzzyVisitor<T> visitor) {
        LinkedList<T> ret = new LinkedList<>();
        for(T choice : choices) {
            if(fuzzyMatches(query, visitor.visit(choice)))
                ret.addFirst(choice);
        }
        return ret;
    }

    public static interface FuzzyVisitor<T> {
        public String visit(T o1);
    }

}
