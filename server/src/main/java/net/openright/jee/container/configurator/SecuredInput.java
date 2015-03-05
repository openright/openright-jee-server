package net.openright.jee.container.configurator;

public interface SecuredInput {

    /** Les input, dekrypter hvis trengs. */
    String decrypt(String input);
}
