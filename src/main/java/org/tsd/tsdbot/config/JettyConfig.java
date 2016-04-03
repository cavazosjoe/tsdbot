package org.tsd.tsdbot.config;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNegative;
import net.sf.oval.constraint.NotNull;

public class JettyConfig {
    @NotEmpty
    @NotNull
    public String hostname;

    @NotNegative
    public int port;
}
