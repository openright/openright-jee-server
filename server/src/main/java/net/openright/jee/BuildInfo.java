package net.openright.jee;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ResourceBundle;

public abstract class BuildInfo {

    private static ResourceBundle buildInfo;
    private static Class<?> defaultSourceClass = BuildInfo.class; // NOSONAR

    public static synchronized void setDefaultSourceClass(Class<?> sourceClass) {
        defaultSourceClass = sourceClass;
    }

    public static String getAppVersjon() {
        return getBuildinfo("app.version");
    }

    public static String getAppName() {
        return getBuildinfo("app.name");
    }

    public static synchronized String getDefaultLocation() {
        return getLocation(defaultSourceClass);
    }

    public static String getLocation(Class<?> sourceClass) {
        ProtectionDomain domain = sourceClass.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();
        return location.toExternalForm();
    }

    private static synchronized String getBuildinfo(String parameterName) {
        if (buildInfo == null) {
            buildInfo = ResourceBundle.getBundle("META-INF/server/build-info");
        }
        String ver = buildInfo.getString(parameterName);
        return ver.matches("\\$\\{.+\\}") ? "DEV" : ver;
    }

    public static boolean isDevelopmentMode() throws IOException {
        // naive check if running in maven folder or in IDE
        File current = new File(new File(".").getCanonicalPath());
        boolean iEclipse = new File(current, "target/classes").exists();
        boolean iMavenBygg = new File(current, "classes").exists() && current.getParent().contains("target");
        return iEclipse || iMavenBygg;
    }

}
