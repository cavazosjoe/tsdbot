package org.tsd.tsdbot.util;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

public class ServletUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
    };

    public static String getIpAddress(HttpServletRequest request) {
        String ip = Arrays.stream(IP_HEADERS)
                .map(request::getHeader)
                .filter(addr -> StringUtils.isNotBlank(addr) && !"unknown".equalsIgnoreCase(addr))
                .findAny().orElse(null);

        if(StringUtils.isNotBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
