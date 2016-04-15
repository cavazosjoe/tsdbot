package org.tsd.tsdbot.config;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

public class TSDFMConfig {

    @NotNull
    @NotEmpty
    public String library;

    @NotNull
    @NotEmpty
    public String target;

    @NotNull
    @NotEmpty
    public String scheduleFile;

    @NotEmpty
    @NotNull
    public String logFile;

}
