package org.tsd.tsdbot.util.fuzzy;

public interface FuzzyVisitor<T> {
    String visit(T o1);
}
