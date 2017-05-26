package org.tsd.tsdbot.config;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

/**
 * For an example file see /resources/tsdbot.example.yml
 */
public class TSDBotConfiguration {

    @NotNull
    public ConnectionConfig connection;

    @NotEmpty
    @NotNull
    public String loggingProperties;

    @NotEmpty
    @NotNull
    public String owner;

    @NotEmpty
    @NotNull
    public String database;

    @NotEmpty
    @NotNull
    public String filenamesDir;

    @NotEmpty
    @NotNull
    public String randomImageDir;

    @NotNull
    public ArchivistConfig archivist;

    @NotNull
    public XBLConfig xbl;

    @NotNull
    public TwitterConfig twitter;

    @NotEmpty
    @NotNull
    public String ffmpegExec;

    @NotEmpty
    @NotNull
    public String ffprobeExec;

    @NotNull
    public TSDFMConfig tsdfm;

    @NotNull
    public TSDTVConfig tsdtv;

    @NotEmpty
    @NotNull
    public String mashapeKey;

    @NotNull
    public GoogleConfig google;

    @NotNull
    public JettyConfig jetty;

    @NotEmpty
    @NotNull
    public String haloApiKey;

    @NotEmpty
    @NotNull
    public String voiceRssApiKey;

    @NotEmpty
    @NotNull
    public String markovFileDirectory;

}
