package org.tsd.tsdbot.config;

import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

public class GoogleConfig {

    @NotEmpty
    @NotNull
    public String clientSecret;

    @NotEmpty
    @NotNull
    public String clientId;

    @NotEmpty
    @NotNull
    public String refreshToken;

    @NotEmpty
    @NotNull
    public String appId;

    @NotEmpty
    @NotNull
    public String apiKey;

    @NotEmpty
    @NotNull
    public String gisCx;
}
