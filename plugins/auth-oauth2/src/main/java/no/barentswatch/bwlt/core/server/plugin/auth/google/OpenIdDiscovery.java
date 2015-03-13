package no.barentswatch.bwlt.core.server.plugin.auth.google;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.json.Json;
import javax.json.JsonReader;

import net.openright.jee.server.plugin.jetty.security.HttpUtil;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

class OpenIdDiscovery {

    private HttpUtil httpUtil;

    public OpenIdDiscovery(HttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    protected OpenIdDiscoveryDocument getDiscoveryDocument(String discoveryDocumentUrl) throws IOException, URISyntaxException {
        URI uri = new URIBuilder(discoveryDocumentUrl)
                .build();
        String body = httpUtil.execute(new HttpGet(uri));
        JsonReader jsonReader = Json.createReader(new StringReader(body));
        
        return OpenIdDiscoveryDocument.from(jsonReader.readObject());
    }

}
