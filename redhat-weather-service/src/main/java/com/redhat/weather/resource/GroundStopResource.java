package com.redhat.weather.resource;

import com.redhat.weather.service.GroundStopService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/weather/ground-stops")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Ground Stops", description = "FAA ground stop and ground delay program operations")
public class GroundStopResource {

    @Inject
    GroundStopService groundStopService;

    @Inject
    MeterRegistry meterRegistry;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/active")
    @Operation(summary = "Get active ground stops", description = "Retrieve all currently active ground stops and GDPs")
    public Response getActiveGroundStops() {
        return Response.ok(groundStopService.getActiveGroundStops())
            .cacheControl(cacheControl(60)).build();
    }

    @GET
    @Path("/airport/{code}")
    @Operation(summary = "Get by airport", description = "Filter ground stops by airport code")
    public Response getByAirport(@PathParam("code") String code) {
        return Response.ok(groundStopService.getByAirport(code))
            .cacheControl(cacheControl(60)).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh ground stops", description = "Manually trigger ground stop data refresh")
    public Response refreshGroundStops() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "ground-stop").increment();
            groundStopService.fetchAndStoreGroundStops();
            return Response.status(Response.Status.ACCEPTED)
                .entity("Ground stop refresh triggered").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh ground stops").build();
        }
    }
}
