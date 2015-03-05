package net.openright.jee.container.starter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class CommandLineInterpreter {

	private boolean waitForInput = true;
	private boolean maskInput = true;

	public String readLineFromConsole(String melding) {
		MaskingThread masker = new MaskingThread(melding);
		if (maskInput) {
			masker.start();
		}

		try {
			InputStreamReader converter = new InputStreamReader(System.in,
					Charset.defaultCharset());
			BufferedReader in = new BufferedReader(converter);
			if (in.ready() || waitForInput) {
				String str = in.readLine();
				return (str == null || str.trim().isEmpty()) ? null : str
						.trim();
			}
			return null;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			masker.stopMasking();
		}
	}

	public CommandLineInterpreter withWaitForInput(boolean waitForInput) {
		this.waitForInput = waitForInput;
		return this;
	}

	public CommandLineInterpreter withMaskInput(boolean maskInput) {
		this.maskInput = maskInput;
		return this;
	}

	public String readLineFromConsole() {
		return readLineFromConsole(null);
	}

	public char[] readLineFromConsoleAsArray(String melding) {
		String key = readLineFromConsole(melding);
		if (key == null || (key = key.trim()).isEmpty()) {
			return null;
		}
		return key.toCharArray();
	}
}

/**
 * Since java.io.Console is not accessible.
 * 
 * @see http://java.sun.com/developer/technicalArticles/Security/pwordmask/
 */
class MaskingThread extends Thread {
	private volatile boolean stop;
	private char echochar = '*';

	public MaskingThread(String prompt) {
		if (prompt != null) {
			System.out.print(prompt); // NOSONAR
		}
	}

	@Override
	public void run() {

		int priority = Thread.currentThread().getPriority();
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		try {
			stop = true;
			while (stop) {
				// Note that deleting characters from console i Eclipse console
				// does not work, but does work in linux
				// See https://bugs.eclipse.no/bugs/show_bug.cgi?id=76936
				System.out.print("\010" + echochar); // NOSONAR
				try {
					Thread.sleep(10);
				} catch (InterruptedException iex) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		} finally { // restore the original priority
			Thread.currentThread().setPriority(priority);
		}
	}

	public void stopMasking() {
		this.stop = false;
	}
}
