package de.ngi.restutils;

import de.ngi.logging.Logger;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.*;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String debugmode = requestContext.getHeaderString("X-Debug");
        if (debugmode != null) {
            String processId = UUID.randomUUID().toString();
            Logger.startProcess(processId);
            Logger.log("Request started: " + requestContext.getUriInfo().getPath());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Logger.endProcess();
    }
}
