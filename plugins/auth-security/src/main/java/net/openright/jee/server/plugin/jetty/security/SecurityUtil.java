package net.openright.jee.server.plugin.jetty.security;

import javax.servlet.http.HttpServletRequest;

public class SecurityUtil {

    public static String getRemoteAddrFromBehindProxy(HttpServletRequest request) {
        String xForwardedFromIp = request.getHeader("X-Forwarded-For");
        if (xForwardedFromIp != null && xForwardedFromIp.contains(",")) {
            xForwardedFromIp = xForwardedFromIp.substring(0, xForwardedFromIp.indexOf(","));
        }

        if (xForwardedFromIp != null) {
            return xForwardedFromIp;
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp == null) {
            return request.getRemoteAddr();
        }
        return xRealIp;
    }
}
