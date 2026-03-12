package com.redhat.weather.resource;

import com.redhat.weather.service.CwaService;
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

@Path("/api/weather/cwas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "CWAs", description = "Center Weather Advisory operations")
public class CwaResource {

    @Inject
    CwaService cwaService;

    @Inject
    MeterRegistry meterRegistry;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/active")
    @Operation(summary = "Get active CWAs", description = "Retrieve all currently active Center Weather Advisories")
    @APIResponse(responseCode = "200", description = "List of active CWAs")
    public Response getActiveCwas() {
        return Response.ok(cwaService.getActiveCwas())
            .cacheControl(cacheControl(120)).build();
    }

    @GET
    @Path("/artcc/{artcc}")
    @Operation(summary = "Get by ARTCC", description = "Filter CWAs by Air Route Traffic Control Center")
    @APIResponse(responseCode = "200", description = "Filtered CWAs by ARTCC")
    public Response getCwasByArtcc(
            @PathParam("artcc") @Parameter(description = "ARTCC identifier (e.g., ZNY)") String artcc) {
        return Response.ok(cwaService.getCwasByArtcc(artcc))
            .cacheControl(cacheControl(120)).build();
    }

    @GET
    @Path("/hazard/{hazard}")
    @Operation(summary = "Get by hazard", description = "Filter CWAs by hazard type")
    @APIResponse(responseCode = "200", description = "Filtered CWAs by hazard type")
    public Response getCwasByHazard(
            @PathParam("hazard") @Parameter(description = "Hazard type (TURB, ICE, IFR, CONVECTIVE)") String hazard) {
        return Response.ok(cwaService.getCwasByHazard(hazard))
            .cacheControl(cacheControl(120)).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh CWAs", description = "Manually trigger CWA data refresh")
    @APIResponse(responseCode = "202", description = "Refresh triggered")
    public Response refreshCwas() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "cwa").increment();
            cwaService.deactivateExpired();
            cwaService.fetchAndStoreCwas();
            return Response.status(Response.Status.ACCEPTED)
                .entity("CWA refresh triggered").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh CWAs").build();
        }
    }
}
