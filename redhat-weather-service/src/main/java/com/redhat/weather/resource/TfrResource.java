package com.redhat.weather.resource;

import com.redhat.weather.service.TfrService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/weather/tfrs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "TFRs", description = "Temporary Flight Restriction operations")
public class TfrResource {

    @Inject
    TfrService tfrService;

    @Inject
    MeterRegistry meterRegistry;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/active")
    @Operation(summary = "Get active TFRs", description = "Retrieve all currently active Temporary Flight Restrictions")
    @APIResponse(responseCode = "200", description = "List of active TFRs")
    public Response getActiveTfrs() {
        return Response.ok(tfrService.getActiveTfrs())
            .cacheControl(cacheControl(300)).build();
    }

    @GET
    @Path("/type/{type}")
    @Operation(summary = "Get by type", description = "Filter TFRs by type (SECURITY, HAZARDS, VIP, SPACE OPERATIONS, etc.)")
    @APIResponse(responseCode = "200", description = "Filtered TFRs by type")
    public Response getTfrsByType(
            @PathParam("type") @Parameter(description = "TFR type (SECURITY, HAZARDS, VIP, SPACE OPERATIONS, SPECIAL)") String type) {
        return Response.ok(tfrService.getTfrsByType(type))
            .cacheControl(cacheControl(300)).build();
    }

    @GET
    @Path("/state/{state}")
    @Operation(summary = "Get by state", description = "Filter TFRs by state")
    @APIResponse(responseCode = "200", description = "Filtered TFRs by state")
    public Response getTfrsByState(
            @PathParam("state") @Parameter(description = "US state abbreviation (e.g., CA, NY)") String state) {
        return Response.ok(tfrService.getTfrsByState(state))
            .cacheControl(cacheControl(300)).build();
    }

    @GET
    @Path("/facility/{facility}")
    @Operation(summary = "Get by ARTCC", description = "Filter TFRs by ARTCC facility")
    @APIResponse(responseCode = "200", description = "Filtered TFRs by ARTCC facility")
    public Response getTfrsByFacility(
            @PathParam("facility") @Parameter(description = "ARTCC facility identifier") String facility) {
        return Response.ok(tfrService.getTfrsByFacility(facility))
            .cacheControl(cacheControl(300)).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh TFRs", description = "Manually trigger TFR data refresh from FAA")
    @APIResponse(responseCode = "202", description = "Refresh triggered")
    public Response refreshTfrs() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "tfr").increment();
            tfrService.fetchAndStoreTfrs();
            return Response.status(Response.Status.ACCEPTED)
                .entity("TFR refresh triggered").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh TFRs").build();
        }
    }
}
