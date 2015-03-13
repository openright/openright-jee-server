package net.openright.java.server.jaxws.example;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.naming.NamingException;

import net.openright.jee.container.configurator.BasicConfigurator;

public class ExampleServerConfigurator extends BasicConfigurator {

    public ExampleServerConfigurator(List<File> configFiles, char[] masterKey) throws IOException {
        super(configFiles, masterKey);
    }

    @Override
    protected boolean addToServletContext(File f) {
        return f.getName().equals("app.properties");
    }

    @Override
    public void initConfiguration() throws NamingException {
        // a good place to initialize datasource and bind to jndi
        // JndiUtil.register("java:comp/env/jdbc/mydb", dataSource);
    }

}
