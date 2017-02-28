package org.tsd.tsdbot.util.fuzzy;

import java.io.File;
import java.util.function.Function;

public class FileVisitor implements Function<File, String> {
    @Override
    public String apply(File file) {
        return file.getName();
    }
}
