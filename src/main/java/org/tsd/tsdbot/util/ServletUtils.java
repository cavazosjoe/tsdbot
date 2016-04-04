package org.tsd.tsdbot.util;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class ServletUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
    };

    public static String getIpAddress(HttpServletRequest request) {
        String ip = null;
        for(String s : IP_HEADERS) {
            ip = request.getHeader(s);
            if(StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip))
                break;
        }
        if(StringUtils.isNotBlank(ip) || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();

        return ip;
    }
}
