package de.fhbielefeld.scl.rest.util;

/**
 *
 * @author Florian
 */
import de.fhbielefeld.scl.logger.Logger;
import de.fhbielefeld.scl.logger.message.Message;
import de.fhbielefeld.scl.logger.message.MessageLevel;
import de.fhbielefeld.smartdata.config.Configuration;
import jakarta.annotation.Priority;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.client.Client;
import org.glassfish.jersey.client.JerseyClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

@Provider
@Priority(Priorities.USER)
public class MirrorRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Configuration conf = new Configuration();

        UriInfo uriInfo = requestContext.getUriInfo();
        String path = uriInfo.getPath();
        String method = requestContext.getMethod();

        String storage = uriInfo.getQueryParameters().getFirst("storage");
        if (storage == null || storage.isBlank()) {
            storage = "public";
        }

        String normalizedPath = path.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
        String normalizedStorage = storage.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
        String normalizedMethod = method.toUpperCase();
        String configKey = normalizedPath + "_" + normalizedStorage + "_" + normalizedMethod + "_url";

        final String mirrorUrl = conf.getProperty(configKey);

        if (mirrorUrl == null || mirrorUrl.isEmpty()) {
            Logger.addMessage(new Message("No mirror target configured for key: >" + configKey + "<", MessageLevel.WARNING));
            return;
        }

        // Body lesen, falls vorhanden
        String body = "";
        InputStream entityStream = requestContext.getEntityStream();
        if (entityStream.available() > 0) {
            Scanner scanner = new Scanner(entityStream, "UTF-8").useDelimiter("\\A");
            if (scanner.hasNext()) {
                body = scanner.next();
            }
        }
        requestContext.setEntityStream(new ByteArrayInputStream(body.getBytes()));

        final String finalBody = body;
        final String finalMethod = method;
        final MediaType mediaType = requestContext.getMediaType() != null
                ? requestContext.getMediaType()
                : MediaType.WILDCARD_TYPE;

        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        final MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        final Map<String, Cookie> cookies = requestContext.getCookies();

        // Asynchroner Mirror-Call
        CompletableFuture.runAsync(() -> {
            try {
                Client client = new JerseyClientBuilder().build();
                WebTarget target = client.target(mirrorUrl);

                // Query-Parameter anhängen
                for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                    for (String value : entry.getValue()) {
                        target = target.queryParam(entry.getKey(), value);
                    }
                }

                Invocation.Builder builder = target.request(mediaType);

                // Header übertragen
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    for (String value : entry.getValue()) {
                        builder.header(entry.getKey(), value);
                    }
                }

                // Cookies übertragen
                for (Map.Entry<String, Cookie> entry : cookies.entrySet()) {
                    builder.cookie(entry.getValue());
                }

                Response mirrorResponse;
                if (finalBody.isEmpty()) {
                    mirrorResponse = builder.method(finalMethod);
                } else {
                    mirrorResponse = builder.method(finalMethod, Entity.entity(finalBody, mediaType));
                }

                Logger.addMessage(new Message("Mirrored request to >" + mirrorUrl + "< with status " + mirrorResponse.getStatus(), MessageLevel.INFO));
            } catch (Exception e) {
                Logger.addMessage(new Message("Failed to mirror request to " + mirrorUrl + ": " + e.getMessage(), MessageLevel.ERROR));
            }
        });
    }
}