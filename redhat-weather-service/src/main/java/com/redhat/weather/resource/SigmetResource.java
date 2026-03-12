package com.redhat.weather.resource;

import com.redhat.weather.service.SigmetService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/weather/sigmets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SIGMETs", description = "SIGMET and AIRMET operations")
public class SigmetResource {

    @Inject
    SigmetService sigmetService;

    @Inject
    MeterRegistry meterRegistry;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/active")
    @Operation(summary = "Get active SIGMETs/AIRMETs", description = "Retrieve all currently active SIGMETs and AIRMETs")
    public Response getActiveSigmets() {
        return Response.ok(sigmetService.getActiveSigmets())
            .cacheControl(cacheControl(120)).build();
    }

    @GET
    @Path("/type/{type}")
    @Operation(summary = "Get by type", description = "Filter by SIGMET or AIRMET type")
    public Response getSigmetsByType(@PathParam("type") String type) {
        return Response.ok(sigmetService.getSigmetsByType(type))
            .cacheControl(cacheControl(120)).build();
    }

    @GET
    @Path("/hazard/{hazard}")
    @Operation(summary = "Get by hazard", description = "Filter by hazard type (ICE, TURB, IFR, CONVECTIVE, etc.)")
    public Response getSigmetsByHazard(@PathParam("hazard") String hazard) {
        return Response.ok(sigmetService.getSigmetsByHazard(hazard))
            .cacheControl(cacheControl(120)).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh SIGMETs", description = "Manually trigger SIGMET/AIRMET data refresh")
    public Response refreshSigmets() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "sigmet").increment();
            sigmetService.deactivateExpired();
            sigmetService.fetchAndStoreSigmets();
            return Response.status(Response.Status.ACCEPTED)
                .entity("SIGMET refresh triggered").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh SIGMETs").build();
        }
    }
}
