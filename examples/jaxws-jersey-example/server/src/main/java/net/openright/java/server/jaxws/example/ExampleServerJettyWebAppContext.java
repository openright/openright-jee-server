package net.openright.java.server.jaxws.example;

import java.io.IOException;

import net.openright.jee.container.jetty.JettyWebAppContext;

public class ExampleServerJettyWebAppContext extends JettyWebAppContext {

    public ExampleServerJettyWebAppContext(String contextPath) throws IOException {
        super(contextPath, ".*example-webapp.+\\.war");
    }

    @Override
    protected String getWebDevHome(String pattern) {
        return "../webapp";
    }

   
}
