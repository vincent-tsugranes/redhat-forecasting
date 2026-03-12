package com.redhat.weather.resource;

import com.redhat.weather.service.VolcanicAshService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/weather/volcanic-ash")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Volcanic Ash", description = "Volcanic ash advisory operations")
public class VolcanicAshResource {

    @Inject
    VolcanicAshService volcanicAshService;

    @Inject
    MeterRegistry meterRegistry;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/active")
    @Operation(summary = "Get active advisories", description = "Retrieve all currently active volcanic ash advisories")
    public Response getActiveAdvisories() {
        return Response.ok(volcanicAshService.getActiveAdvisories())
            .cacheControl(cacheControl(120)).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh volcanic ash advisories", description = "Manually trigger volcanic ash advisory refresh")
    public Response refreshAdvisories() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "volcanic-ash").increment();
            volcanicAshService.deactivateExpired();
            volcanicAshService.fetchAndStoreAdvisories();
            return Response.status(Response.Status.ACCEPTED)
                .entity("Volcanic ash advisory refresh triggered").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh volcanic ash advisories").build();
        }
    }
}
