package com.redhat.weather.resource;

import com.redhat.weather.service.PirepService;
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

@Path("/api/weather/pireps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "PIREPs", description = "Pilot Reports (PIREPs) operations")
public class PirepResource {

    @Inject
    PirepService pirepService;

    @Inject
    MeterRegistry meterRegistry;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/recent")
    @Operation(summary = "Get recent PIREPs", description = "Retrieve PIREPs from the last 3 hours")
    @APIResponse(responseCode = "200", description = "List of recent PIREPs")
    public Response getRecentPireps() {
        return Response.ok(pirepService.getRecentPireps())
            .cacheControl(cacheControl(120)).build();
    }

    @GET
    @Path("/turbulence/{intensity}")
    @Operation(summary = "Get PIREPs by turbulence", description = "Filter PIREPs by turbulence intensity (NEG, LGT, MOD, SEV, EXTRM)")
    @APIResponse(responseCode = "200", description = "Filtered PIREPs by turbulence intensity")
    public Response getPirepsByTurbulence(
            @PathParam("intensity") @Parameter(description = "Turbulence intensity (NEG, LGT, MOD, SEV, EXTRM)") String intensity) {
        return Response.ok(pirepService.getPirepsByTurbulence(intensity))
            .cacheControl(cacheControl(120)).build();
    }

    @GET
    @Path("/icing/{intensity}")
    @Operation(summary = "Get PIREPs by icing", description = "Filter PIREPs by icing intensity (NEG, TRC, LGT, MOD, SEV)")
    @APIResponse(responseCode = "200", description = "Filtered PIREPs by icing intensity")
    public Response getPirepsByIcing(
            @PathParam("intensity") @Parameter(description = "Icing intensity (NEG, TRC, LGT, MOD, SEV)") String intensity) {
        return Response.ok(pirepService.getPirepsByIcing(intensity))
            .cacheControl(cacheControl(120)).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh PIREPs", description = "Manually trigger PIREP data refresh")
    @APIResponse(responseCode = "202", description = "Refresh triggered")
    public Response refreshPireps() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "pirep").increment();
            pirepService.fetchAndStorePireps();
            return Response.status(Response.Status.ACCEPTED)
                .entity("PIREP refresh triggered").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh PIREPs").build();
        }
    }
}
