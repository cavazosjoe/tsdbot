package org.tsd.tsdbot.util;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Joe on 2/2/2015.
 */
public class ServletUtils {

    public static String getIpAddress(HttpServletRequest request) {
        String[] headersToCheck = {
                "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };
        String ip = null;
        for(String s : headersToCheck) {
            ip = request.getHeader(s);
            if(StringUtils.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip))
                break;
        }
        if(StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();

        return ip;
    }
}
