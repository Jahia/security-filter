package org.jahia.modules.securityfilter.api;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.slf4j.LoggerFactory.getLogger;


@Component
@Path("/security-filter")
@Produces({"application/hal+json"})
public class SecurityFilterAPI {
    private static final Logger logger = getLogger(SecurityFilterAPI.class);

    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHello() {
        return Response.status(Response.Status.OK).entity("{\"success\":\"Hello from SecurityFilterAPI!\"}").build();
    }
}
