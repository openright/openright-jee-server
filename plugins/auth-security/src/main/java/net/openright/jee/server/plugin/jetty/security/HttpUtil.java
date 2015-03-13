package net.openright.jee.server.plugin.jetty.security;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;

public class HttpUtil {
    private HttpClient httpClient;

    public HttpUtil(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Makes request and checks response code for 200
     */
    public String execute(HttpRequestBase request) throws ParseException, IOException {
        HttpResponse response = httpClient.execute(request);

        HttpEntity entity = response.getEntity();
        String body = EntityUtils.toString(entity);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new ConnectException("Expected 200 but got " + response.getStatusLine().getStatusCode() + ", with body " + body);
        }

        return body;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

}
