package no.barentswatch.bwlt.core.server.plugin.auth.google;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * OpenId Connect protocol discovery document.
 * 
 * @see https://developers.google.com/accounts/docs/OAuth2Login#discovery
 */
class OpenIdDiscoveryDocument {

    private String issuer;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;
    private String revocationEndpoint;
    private String jwksUri;
    private List<String> responseTypesSupported = new ArrayList<>();
    private List<String> subjectTypesSupported = new ArrayList<>();
    private List<String> idTokenAlgValuesSupported = new ArrayList<>();
    private static JsonObject json;

    public OpenIdDiscoveryDocument() {
    }

    public static OpenIdDiscoveryDocument from(JsonObject json) {

        OpenIdDiscoveryDocument.json = json;
        OpenIdDiscoveryDocument doc = new OpenIdDiscoveryDocument();

        doc.issuer = json.getString("issuer");
        doc.authorizationEndpoint = json.getString("authorization_endpoint");
        doc.tokenEndpoint = json.getString("token_endpoint");
        doc.userInfoEndpoint = json.getString("userinfo_endpoint");
        doc.revocationEndpoint = json.getString("revocation_endpoint");
        doc.jwksUri = json.getString("jwks_uri");

        readArray(json.getJsonArray("response_types_supported"), doc.responseTypesSupported);
        readArray(json.getJsonArray("response_types_supported"), doc.subjectTypesSupported);
        readArray(json.getJsonArray("id_token_alg_values_supported"), doc.idTokenAlgValuesSupported);
        return doc;
    }

    protected static void readArray(JsonArray json, List<String> targetList) {
        if (json != null) {
            for (JsonValue v : json) {
                targetList.add(((JsonString) v).getString());
            }
        }
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public String getRevocationEndpoint() {
        return revocationEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public List<String> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public List<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public List<String> getIdTokenAlgValuesSupported() {
        return idTokenAlgValuesSupported;
    }

    @Override
    public String toString() {
        if (json != null) {
            StringWriter writer = new StringWriter();
            Json.createWriter(writer).write(json);
            return getClass().getSimpleName() + "<" + writer.toString() + ">";
        } else {
            return getClass().getSimpleName() + "<>";
        }
    }
}
