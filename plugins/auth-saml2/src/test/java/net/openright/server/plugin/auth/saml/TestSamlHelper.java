package net.openright.server.plugin.auth.saml;

import static org.fest.assertions.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml2.core.Assertion;

public class TestSamlHelper {
    @BeforeClass
    public static void init() throws Exception {
        SamlHelper.init();
    }

    @Test
    public void getAssertionAttributes() throws Exception {
        Path assertionPath = Paths.get(getClass().getResource("/SAMLAssertion.xml").toURI());
        String assertionString = new String(Files.readAllBytes(assertionPath), StandardCharsets.UTF_8);
        Assertion assertion = SamlHelper.parseAssertion(assertionString);

        assertThat(SamlHelper.getAttributeString("uid", assertion)).isEqualTo("somesillyuid");
    }
}
