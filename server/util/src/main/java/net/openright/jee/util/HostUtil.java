package net.openright.jee.util;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public abstract class HostUtil {

    private static final String HTTP_PORT = "app.http.port";
    public static final int DAYTIME_PORT = 13;

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    public static String getPid() {
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    public static boolean ping(String host) {
        return ping(host, DAYTIME_PORT);
    }

    public static boolean ping(String host, int port) {
        return ping(host, port, 500);
    }

    public static boolean ping(String host, int port, int timeout) {
        try {
            return new InetSocketAddress(InetAddress.getByName(host), port).getAddress().isReachable(timeout);
        } catch (UnknownHostException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getHttpPort() {
        return System.getProperty(HTTP_PORT);
    }

    public static String uniqueInstanceId() {
        return getHostName() + ":" + getHttpPort();
    }
}
