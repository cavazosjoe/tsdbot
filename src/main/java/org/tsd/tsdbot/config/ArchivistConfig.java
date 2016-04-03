package org.tsd.tsdbot.config;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

public class ArchivistConfig {
    @NotNull
    @NotEmpty
    public String logsDir;
}
