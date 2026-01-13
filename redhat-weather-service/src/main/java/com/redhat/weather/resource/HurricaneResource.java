package com.redhat.weather.resource;

import com.redhat.weather.domain.entity.HurricaneEntity;
import com.redhat.weather.service.HurricaneService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Path("/api/weather/hurricanes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Hurricanes", description = "Hurricane and tropical storm operations")
public class HurricaneResource {

    @Inject
    HurricaneService hurricaneService;

    @GET
    @Path("/active")
    @Operation(summary = "Get active storms", description = "Retrieve all currently active tropical systems")
    @APIResponse(responseCode = "200", description = "List of active storms")
    public Response getActiveStorms() {
        List<HurricaneEntity> storms = hurricaneService.getActiveStorms();
        return Response.ok(storms).build();
    }

    @GET
    @Path("/{stormId}")
    @Operation(summary = "Get storm by ID", description = "Retrieve all advisories for a specific storm")
    @APIResponse(responseCode = "200", description = "List of storm advisories")
    @APIResponse(responseCode = "404", description = "Storm not found")
    public Response getStormById(
            @PathParam("stormId") @Parameter(description = "Storm ID (e.g., AL012024)") String stormId) {

        List<HurricaneEntity> storm = hurricaneService.getStormById(stormId);

        if (storm.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("No storm found with ID: " + stormId)
                .build();
        }

        return Response.ok(storm).build();
    }

    @GET
    @Path("/{stormId}/track")
    @Operation(summary = "Get storm track", description = "Retrieve storm track for a specific time range")
    @APIResponse(responseCode = "200", description = "Storm track data")
    @APIResponse(responseCode = "400", description = "Invalid parameters")
    @APIResponse(responseCode = "404", description = "Storm not found")
    public Response getStormTrack(
            @PathParam("stormId") @Parameter(description = "Storm ID") String stormId,
            @QueryParam("from") @Parameter(description = "Start time (ISO-8601)") String from,
            @QueryParam("to") @Parameter(description = "End time (ISO-8601)") String to) {

        try {
            LocalDateTime fromTime = from != null ?
                LocalDateTime.parse(from, DateTimeFormatter.ISO_DATE_TIME) :
                LocalDateTime.now().minusDays(7);

            LocalDateTime toTime = to != null ?
                LocalDateTime.parse(to, DateTimeFormatter.ISO_DATE_TIME) :
                LocalDateTime.now();

            List<HurricaneEntity> track = hurricaneService.getStormTrack(stormId, fromTime, toTime);

            if (track.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("No track data found for storm: " + stormId)
                    .build();
            }

            return Response.ok(track).build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid date format. Use ISO-8601 format (e.g., 2024-01-15T10:00:00)")
                .build();
        }
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh hurricane data", description = "Manually trigger a refresh of hurricane data")
    @APIResponse(responseCode = "202", description = "Refresh triggered")
    public Response refreshHurricaneData() {
        try {
            hurricaneService.fetchAndStoreActiveStorms();

            return Response.status(Response.Status.ACCEPTED)
                .entity("Hurricane data refresh triggered")
                .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh hurricane data")
                .build();
        }
    }
}
