package org.tsd.tsdbot.warzone.library;

public interface StringGenStrategy<T extends GeneratorParams> {
    String gen(StringBuilder builder, T params);
}
