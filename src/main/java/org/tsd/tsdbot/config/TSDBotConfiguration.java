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
    public String owner;

    @NotEmpty
    @NotNull
    public String database;

    @NotNull
    public ArchivistConfig archivist;

    @NotNull
    public XBLConfig xbl;

    @NotNull
    public TwitterConfig twitter;

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

}
