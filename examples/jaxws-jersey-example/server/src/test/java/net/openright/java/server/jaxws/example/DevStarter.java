package net.openright.java.server.jaxws.example;


import java.io.IOException;

import net.openright.java.server.jaxws.example.ExampleServerJettyWebAppContext;
import net.openright.java.server.jaxws.example.Starter;
import net.openright.jee.container.jetty.JettyWebAppContext;

import org.eclipse.jetty.util.resource.ResourceCollection;

class DevStarter extends Starter {

    void start() throws Exception {
        super.start(new String[] { "restart" });
    }

    @Override
    protected JettyWebAppContext createWebAppContext(String contextPath) throws IOException {
        return new ExampleServerJettyWebAppContext(contextPath) {

            @Override
            protected void initContextForIde(String contextPath) {
                super.initContextForIde(contextPath);
                initClientStaticResources();
            }

            void initClientStaticResources() {
                // turn off all caching of static resources, so we can change them on the fly in development
                setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");

                ResourceCollection base = new ResourceCollection(new String[] { getWar(), "../webapp/src/main/webapp" });
                setBaseResource(base);
            }
        };
    }
}
