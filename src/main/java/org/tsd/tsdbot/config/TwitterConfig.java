package org.tsd.tsdbot.config;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

public class TwitterConfig {
    @NotEmpty
    @NotNull
    public String consumerKey;

    @NotEmpty
    @NotNull
    public String consumerKeySecret;

    @NotEmpty
    @NotNull
    public String accessToken;

    @NotEmpty
    @NotNull
    public String accessTokenSecret;

}
