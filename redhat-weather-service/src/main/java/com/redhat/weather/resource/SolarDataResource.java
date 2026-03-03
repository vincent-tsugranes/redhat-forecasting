package com.redhat.weather.resource;

import com.redhat.weather.dto.SolarDataDTO;
import com.redhat.weather.service.SolarDataService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/weather/solar")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Solar Data", description = "Sunrise/sunset and solar information")
public class SolarDataResource {

    @Inject
    SolarDataService solarDataService;

    @GET
    @Path("/{locationId}")
    @Operation(summary = "Get solar data for a location",
               description = "Retrieve sunrise, sunset, and day length for a specific location")
    @APIResponse(responseCode = "200", description = "Solar data")
    @APIResponse(responseCode = "404", description = "Solar data unavailable")
    public Response getSolarData(
            @PathParam("locationId") @Parameter(description = "Location ID") Long locationId) {

        return solarDataService.getSolarData(locationId)
            .map(dto -> Response.ok(dto).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity("Solar data unavailable for this location")
                .build());
    }
}
