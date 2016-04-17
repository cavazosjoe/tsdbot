package org.tsd.tsdbot.util.fuzzy;

import java.io.File;

public class FileVisitor implements FuzzyVisitor<File> {
    @Override
    public String visit(File o1) {
        return o1.getName();
    }
}
