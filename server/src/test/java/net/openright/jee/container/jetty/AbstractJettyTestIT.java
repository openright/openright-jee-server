package net.openright.jee.container.jetty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.BeforeClass;

public abstract class AbstractJettyTestIT {

	private static final Path logdir = new File("logs").toPath();

	@BeforeClass
	public static void createLogDir() throws IOException {
		Files.createDirectories(logdir);
	}

}
