package net.openright.java.server.jaxws.example.api;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api/*")
public class JaxRsApplication extends ResourceConfig {
    public JaxRsApplication() {
        packages("net.openright.jee.server.example.api");
    }
}
