package net.openright.jee.server.plugin.jetty.security;

import java.time.OffsetDateTime;

import javax.servlet.http.HttpServletRequest;

public class AuthenticationEvent {

    private String remoteAddress;

    private final OffsetDateTime eventTime = OffsetDateTime.now();

    public AuthenticationEvent() {
    }

    public AuthenticationEvent(HttpServletRequest request) {
        this.remoteAddress = SecurityUtil.getRemoteAddrFromBehindProxy(request);
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public OffsetDateTime getEventTime() {
        return eventTime;
    }

}
