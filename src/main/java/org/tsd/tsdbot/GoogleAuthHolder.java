package org.tsd.tsdbot;

import java.util.Properties;

/**
 * Created by Joe on 4/29/2015.
 */
public class GoogleAuthHolder {

    private String appId;
    private String clientId;
    private String clientSecret;
    private String refreshToken;

    public GoogleAuthHolder(Properties properties) {
        this.appId = properties.getProperty("google.appId");
        this.clientId = properties.getProperty("google.clientId");
        this.clientSecret = properties.getProperty("google.clientSecret");
        this.refreshToken = properties.getProperty("google.refreshToken");
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
