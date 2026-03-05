package com.redhat.weather.resource;

import com.redhat.weather.service.ClimateNormalsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/weather/climate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Climate Normals", description = "Historical climate averages")
public class ClimateNormalsResource {

    @Inject
    ClimateNormalsService climateNormalsService;

    @GET
    @Path("/{locationId}")
    @Operation(summary = "Get climate normals for a location",
               description = "Retrieve historical average temperatures, precipitation, and wind for a location")
    @APIResponse(responseCode = "200", description = "Climate normals data")
    @APIResponse(responseCode = "404", description = "Insufficient data for normals computation")
    public Response getClimateNormals(
            @PathParam("locationId") @Parameter(description = "Location ID") Long locationId) {
        return climateNormalsService.getClimateNormals(locationId)
            .map(dto -> Response.ok(dto).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity("Insufficient historical data for climate normals")
                .build());
    }
}
