package com.redhat.weather.resource;

import com.redhat.weather.service.WindsAloftService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/weather/winds-aloft")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Winds Aloft", description = "Winds and temperatures aloft forecast operations")
public class WindsAloftResource {

    @Inject
    WindsAloftService windsAloftService;

    @Inject
    MeterRegistry meterRegistry;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/latest")
    @Operation(summary = "Get latest winds aloft", description = "Retrieve the most recent winds/temps aloft forecasts")
    public Response getLatestWinds() {
        return Response.ok(windsAloftService.getLatestWinds())
            .cacheControl(cacheControl(300)).build();
    }

    @GET
    @Path("/station/{stationId}")
    @Operation(summary = "Get by station", description = "Get winds aloft forecasts for a specific station")
    public Response getWindsByStation(@PathParam("stationId") String stationId) {
        return Response.ok(windsAloftService.getWindsByStation(stationId))
            .cacheControl(cacheControl(300)).build();
    }

    @GET
    @Path("/altitude/{altitudeFt}")
    @Operation(summary = "Get by altitude", description = "Get winds aloft at a specific flight level (e.g., 3000, 6000, 9000)")
    public Response getWindsByAltitude(@PathParam("altitudeFt") int altitudeFt) {
        return Response.ok(windsAloftService.getWindsByAltitude(altitudeFt))
            .cacheControl(cacheControl(300)).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh winds aloft", description = "Manually trigger winds aloft data refresh")
    public Response refreshWindsAloft() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "winds-aloft").increment();
            windsAloftService.fetchAndStoreWinds();
            return Response.status(Response.Status.ACCEPTED)
                .entity("Winds aloft refresh triggered").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh winds aloft").build();
        }
    }
}
