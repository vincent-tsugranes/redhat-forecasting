package com.redhat.weather.resource;

import com.redhat.weather.service.LightningService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/weather/lightning")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Lightning", description = "Lightning strike data operations")
public class LightningResource {

    @Inject
    LightningService lightningService;

    @Inject
    MeterRegistry meterRegistry;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/recent")
    @Operation(summary = "Get recent lightning strikes", description = "Retrieve lightning strikes from the past hour")
    public Response getRecentStrikes() {
        return Response.ok(lightningService.getRecentStrikes())
            .cacheControl(cacheControl(30)).build();
    }

    @GET
    @Path("/count")
    @Operation(summary = "Get recent strike count", description = "Count of lightning strikes in the past hour")
    public Response getRecentCount() {
        return Response.ok(lightningService.getRecentCount())
            .cacheControl(cacheControl(30)).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh lightning data", description = "Manually trigger lightning data refresh")
    public Response refreshLightning() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "lightning").increment();
            lightningService.fetchAndStoreStrikes();
            return Response.status(Response.Status.ACCEPTED)
                .entity("Lightning data refresh triggered").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh lightning data").build();
        }
    }
}
