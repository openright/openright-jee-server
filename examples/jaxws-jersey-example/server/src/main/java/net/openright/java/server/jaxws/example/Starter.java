package net.openright.java.server.jaxws.example;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.openright.jee.container.configurator.BasicConfigurator;
import net.openright.jee.container.jetty.JettyWebAppContext;
import net.openright.jee.container.starter.AbstractApplicationStarter;

public class Starter extends AbstractApplicationStarter {

    public static void main(String[] args) throws Exception { // NOSONAR
        new Starter().start(args);
    }

    @Override
    protected BasicConfigurator createConfiguration(List<File> defaultConfigFiles, char[] masterKey) throws IOException {
        return new ExampleServerConfigurator(defaultConfigFiles, masterKey);
    }

    @Override
    protected JettyWebAppContext createWebAppContext(String contextPath) throws IOException {
        return new ExampleServerJettyWebAppContext(contextPath);
    }

    @Override
    protected void extractWarFile() throws Exception { // NOSONAR
        System.out.println("Unpacking war file"); // NOSONAR
        new ExampleServerJettyWebAppContext("").unpack();
    }

}
