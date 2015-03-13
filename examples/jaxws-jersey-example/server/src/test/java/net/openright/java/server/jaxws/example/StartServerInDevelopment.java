package net.openright.java.server.jaxws.example;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import net.openright.jee.container.configurator.PropertyUtil;

/**
 * Simple class for starting server from IDE with property files for test
 */
public class StartServerInDevelopment {

    public static void main(final String[] args) throws Exception {
        PropertyUtil.setProperty("app.propertyfile", PropertyUtil.getProperty("app.propertyfile", "./conf/app.properties"));

        String configname = PropertyUtil.getProperty("app.propertyfile");

        File appPropertyFile = new File(configname);
        initConfigFile(appPropertyFile, new File("./src/test/resources/app-test.properties"));
        initConfigFile(new File("./conf/logback-config.xml"), new File("./src/test/resources/logback-test-config.xml"));

        new DevStarter().start();
    }

    private static void initConfigFile(File targetFile, File templateFile) throws IOException {
        if (templateFile.exists() && (!targetFile.exists() || targetFile.lastModified() < templateFile.lastModified())) {
            File parentDir = targetFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                throw new IllegalArgumentException("Could not create directory " + parentDir.getAbsolutePath());
            }
            Files.copy(templateFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
