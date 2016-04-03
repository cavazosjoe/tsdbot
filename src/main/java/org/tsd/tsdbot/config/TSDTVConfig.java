package org.tsd.tsdbot.config;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

public class TSDTVConfig {
    @NotEmpty
    @NotNull
    public String ffmpegExec;

    @NotEmpty
    @NotNull
    public String ffmpegArgs;

    @NotEmpty
    @NotNull
    public String ffmpegOut;

    @NotEmpty
    @NotNull
    public String directLink;

    @NotEmpty
    @NotNull
    public String videoFmt;

    @NotEmpty
    @NotNull
    public String catalog;

    @NotEmpty
    @NotNull
    public String raws;

    @NotEmpty
    @NotNull
    public String scheduleFile;

    @NotEmpty
    @NotNull
    public String logFile;
}
