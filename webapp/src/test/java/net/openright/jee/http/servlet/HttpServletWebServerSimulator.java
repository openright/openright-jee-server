package net.openright.jee.http.servlet;

import static org.fest.assertions.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class to simulate very simple web server for testing purposes. */
public class HttpServletWebServerSimulator extends HttpServlet {

	static class WebServerSimulatorData {
		private final List<String> messagesWhichCanBeRetrieved = new ArrayList<>();
		private final LinkedBlockingQueue<String> messagesWhichHaveBeenPosted = new LinkedBlockingQueue<>();
		int status = HttpServletResponse.SC_OK;
		String statusMelding;
		HttpServlet servlet;

		void stop() {
			if (servlet != null) {
				servlet.destroy();
			}
		}
	}

	private SimpleHttpServletWebServer server;
	private final WebServerSimulatorData data = new WebServerSimulatorData();
	private final String contextPath;

	public HttpServletWebServerSimulator() {
		this("/");
	}

	HttpServletWebServerSimulator(final String contextPath) {
		this.contextPath = contextPath;
	}

	public void leggTilMelding(String melding) {
		this.data.messagesWhichCanBeRetrieved.add(melding);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (data.messagesWhichCanBeRetrieved.isEmpty()) {
			if (data.statusMelding != null) {
				setResponseStatus(resp);
			} else {
				resp.sendError(404);
			}
		} else {
			resp.getWriter().append(data.messagesWhichCanBeRetrieved.remove(0));
			setResponseStatus(resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		data.messagesWhichHaveBeenPosted.add(toString(req.getReader()));
		setResponseStatus(resp);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (data.servlet != null) {
			data.servlet.service(req, resp);
		} else {
			super.service(req, resp);
		}
	}

	protected void setResponseStatus(HttpServletResponse resp)
			throws IOException {
		if (data.status < 400) {
			resp.setStatus(data.status);
		} else if (data.statusMelding != null) {
			resp.sendError(data.status, data.statusMelding);
		} else {
			resp.sendError(data.status);
		}
	}

	public void assertCountMessagesAccepted(int i) {
		assertThat(data.messagesWhichHaveBeenPosted).hasSize(i);
	}

	public String takeOnlyMessage() throws InterruptedException {
		assertThat(data.messagesWhichHaveBeenPosted).hasSize(1);
		return data.messagesWhichHaveBeenPosted.take();
	}

	public String waitForOnlyMessage(long timeout, TimeUnit unit)
			throws InterruptedException {
		assertThat(data.messagesWhichHaveBeenPosted.size()).as(
				data.messagesWhichHaveBeenPosted.toString())
				.isLessThanOrEqualTo(1);
		return data.messagesWhichHaveBeenPosted.poll(timeout, unit);
	}

	public synchronized void startServer(int port) throws Exception {
		if (server == null) {
			server = new SimpleHttpServletWebServer(contextPath, this);
			server.start(port);
		}
	}

	public synchronized void stopServer() throws Exception {
		if (data != null) {
			data.stop();
		}
		if (server != null) {
			server.stop(0);
		}
	}

	public synchronized String getUrl() {
		return "http://localhost:" + getServerPort() + "/";
	}

	public synchronized int getServerPort() {
		return server.getServerPort();
	}

	public void simulateResponseStatus(int status) {
		simulateResponseStatus(status, null);
	}

	public void simulateResponseStatus(int status, String statusMelding) {
		this.data.status = status;
		this.data.statusMelding = statusMelding;
	}

	public void setServlet(HttpServlet servlet) {
		this.data.servlet = servlet;
	}

	private static String toString(BufferedReader reader) throws IOException {
		StringBuilder buffer = new StringBuilder(1024);
		char[] tmp = new char[1024];
		int l;
		while ((l = reader.read(tmp)) != -1) {
			buffer.append(tmp, 0, l);
		}
		return buffer.toString();
	}

}
