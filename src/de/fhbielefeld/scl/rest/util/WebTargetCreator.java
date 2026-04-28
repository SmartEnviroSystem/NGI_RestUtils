package de.fhbielefeld.scl.rest.util;

import de.fhbielefeld.scl.logger.Logger;
import de.fhbielefeld.scl.logger.message.Message;
import de.fhbielefeld.scl.logger.message.MessageLevel;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

//import org.glassfish.jersey.client.filter.EncodingFilter;
//import org.glassfish.jersey.message.GZipEncoder;
/**
 * Creates web targets baseing on the base_uri for all REST webservices
 *
 * @author ssteinmeyer, ffehring
 */
public class WebTargetCreator {

    private URI baseUri;

    /**
     * Creates a new WebTargetCreator
     *
     * @param base_url Servers URL where the REST Webservice is located on
     */
    public WebTargetCreator(String base_url) {
        try {
            this.baseUri = this.transformURLtoURI(new URL(base_url));
        } catch (MalformedURLException ex) {
            Message msg = new Message("Could not build webtarget of >"
                    + base_url + "< because of wrong URL syntax: " + ex.getLocalizedMessage(), MessageLevel.ERROR);
            Logger.addMessage(msg);
        }
    }

    /**
     * Creates a new WebTargetCreator
     *
     * @param base_url Servers URL where the REST Webservice is located on
     */
    public WebTargetCreator(URL base_url) {
        this.baseUri = transformURLtoURI(base_url);
    }

    /**
     * Transforms the given URL to URI.
     *
     * @param url URL to transform
     * @return URI object
     */
    private URI transformURLtoURI(URL url) {
        try {
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        } catch (URISyntaxException ex) {
            Message msg = new Message("Could not build webtarget from url >"
                    + url + "< because of wrong URL syntax: " + ex.getLocalizedMessage(), MessageLevel.ERROR);
            Logger.addMessage(msg);
        }
        return null;
    }

    /**
     * Creates a webtarget to the specified resource
     *
     * @param resource Name of the resource where a webtarget should state
     * @return WebTarget to the given resource
     */
    public WebTarget createWebTarget(String resource, boolean insecure) {
        WebTarget webTarget;
        Client client;
        if (insecure) {
            client = ClientBuilder.newBuilder()
                    .sslContext(createInsecureSSLContext())
                    .hostnameVerifier(insecureHostnameVerifier())
                    .build();
        } else {
            client = ClientBuilder.newClient();
        }
//        client.register(MultiPartFeature.class);
//        client.register(EncodingFilter.class);
//        client.register(GZipEncoder.class);
//            client.property(ClientProperties.USE_ENCODING, "gzip");
        webTarget = client.target(this.baseUri.toASCIIString()).path(resource);
        return webTarget;
    }

    /**
     * Creates a webtarget to the given resource
     *
     * @param base_url Servers URL where the REST Webservice is located on
     * @param resource Name of the resource
     * @return WebTarget to the resource
     */
    public static WebTarget createWebTarget(String base_url, String resource) {
        WebTargetCreator creator = new WebTargetCreator(base_url);
        return creator.createWebTarget(resource, false);
    }
    
    /**
     * Creates a webtarget to the given resource with possibility to use a 
     * unsecure connection.
     *
     * @param base_url Servers URL where the REST Webservice is located on
     * @param resource Name of the resource
     * @return WebTarget to the resource
     */
    public static WebTarget createWebTarget(String base_url, String resource, boolean insecure) {
        WebTargetCreator creator = new WebTargetCreator(base_url);
        return creator.createWebTarget(resource, insecure);
    }

    private static SSLContext createInsecureSSLContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static HostnameVerifier insecureHostnameVerifier() {
        return (hostname, session) -> true;
    }
}
