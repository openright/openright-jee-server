package net.openright.jee.container.starter;

import java.security.GeneralSecurityException;

import net.openright.jee.security.MasterKeyCrypto;

/**
 * Handles encryption/decryption of properties based on a master password which
 * is entered through console.
 */
public class Crypto {

	/**
	 * Reads master password from command line and encrypts or decrypts value.
	 * @param args
	 *            [encrypt|decrypt]
	 */
	public static void main(String[] args) throws GeneralSecurityException {
		if (args == null || args.length != 1) {
			throw new IllegalArgumentException(
					"Must specify options: [encrypt|decrypt]");
		}

		boolean encrypt = "encrypt".equals(args[0]);

		CommandLineInterpreter CommandLineInterpreter = new CommandLineInterpreter()
				.withMaskInput(true).withWaitForInput(true);
		byte[] masterPassord = MasterKeyCrypto
				.convertCharToBytesUTF(CommandLineInterpreter
						.readLineFromConsoleAsArray("Master password:"));
		if (encrypt) {
			String verdi1 = CommandLineInterpreter
					.readLineFromConsole("Input value: ");
			String verdi2 = CommandLineInterpreter
					.readLineFromConsole("Repeat value: ");
			if (!verdi1.equals(verdi2)) {
				throw new IllegalArgumentException(
						"Input does not match. Cannot continue.");
			}
			System.out.println("Encrypted: "
					+ MasterKeyCrypto.encrypt(verdi1, masterPassord)); // NOSONAR
		} else {
			String verdi = new CommandLineInterpreter().withMaskInput(false)
					.withWaitForInput(true).readLineFromConsole("Input value:"); // NOSONAR
			System.out.println("Decrypted: "
					+ MasterKeyCrypto.decrypt(verdi, masterPassord)); // NOSONAR
		}
	}

}
