package org.tsd.tsdbot.config;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

public class XBLConfig {

    @NotEmpty
    @NotNull
    public String apiKey;

    @NotEmpty
    @NotNull
    public String xuid;

}
