package org.tsd.tsdbot;

import org.tsd.tsdbot.config.GoogleConfig;

/**
 * Created by Joe on 4/29/2015.
 */
public class GoogleAuthHolder {

    private String appId;
    private String clientId;
    private String clientSecret;
    private String refreshToken;

    public GoogleAuthHolder(GoogleConfig config) {
        this.appId = config.appId;
        this.clientId = config.clientId;
        this.clientSecret = config.clientSecret;
        this.refreshToken = config.refreshToken;
    }

    public String getAppId() {
        return appId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
