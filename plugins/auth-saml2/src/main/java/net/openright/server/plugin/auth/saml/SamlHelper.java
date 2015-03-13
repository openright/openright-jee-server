package net.openright.server.plugin.auth.saml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.schema.XSString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SamlHelper {
    private static StaticBasicParserPool ppMgr;

    public static void init() throws ConfigurationException, XMLParserException {
        // Initialize the library
        DefaultBootstrap.bootstrap();

        // Get parser pool manager
        ppMgr = new StaticBasicParserPool();
        ppMgr.setNamespaceAware(true);
        ppMgr.initialize();
    }
    
    public static Assertion parseAssertion(String assertionString) throws XMLParserException, UnmarshallingException {
        Document assertionDoc = ppMgr.parse(new StringReader(assertionString));
        Element assertionRoot = assertionDoc.getDocumentElement();

        Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(assertionRoot);
        Assertion assertion = (Assertion) unmarshaller.unmarshall(assertionRoot);
        return assertion;
    }

    public static String getAttributeString(String attributeName, Assertion assertion) {
        return ((XSString) getAttributeValue(attributeName, XSString.TYPE_NAME, assertion)).getValue();
    }

    private static XMLObject getAttributeValue(String attributeName, QName schemaType, Assertion assertion) {
        List<XMLObject> attributeValues = getAttributeValues(attributeName, schemaType, assertion);
        if(attributeValues.size() > 1){
            throw new IllegalStateException("Assertion contains more than one " + attributeName);
        }
        if(attributeValues.isEmpty()){
            throw new IllegalStateException("Assertion doesn't contain any " + attributeName);
        }
        return attributeValues.get(0);
    }

    private static List<XMLObject> getAttributeValues(String attributeName, QName schemaType, Assertion assertion) {
        List<XMLObject> attributeValues = getAttributeValues(attributeName, assertion);
        for (XMLObject attribVal : attributeValues) {
            if(!(schemaType.equals(attribVal.getSchemaType()))){
                throw new IllegalStateException(attributeName + " is expected to be of type: \"" + schemaType + "\", but was: \"" + attribVal.getSchemaType() +"\"");
            }
        }
        return attributeValues;
    }

    public static XMLObject getAttributeValue(String attributeName, Assertion assertion) {
        List<XMLObject> attributeValues = getAttributeValues(attributeName, assertion);
        if(attributeValues.size() > 1){
            throw new IllegalStateException("Assertion contains more than one " + attributeName);
        }
        if(attributeValues.isEmpty()){
            throw new IllegalStateException("Assertion doesn't contain any " + attributeName);
        }
        return attributeValues.get(0);
    }

    public static List<XMLObject> getAttributeValues(String attributeName, Assertion assertion) {
        List<XMLObject> values = new ArrayList<XMLObject>();
        for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            for (Attribute attribute : attributeStatement.getAttributes()) {
                if(attribute.getName().equals(attributeName)){
                    values.addAll(attribute.getAttributeValues());
                }
            }
        }
        return values;
    }
}
