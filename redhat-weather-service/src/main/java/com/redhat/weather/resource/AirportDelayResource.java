package com.redhat.weather.resource;

import com.redhat.weather.service.AirportDelayService;
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

@Path("/api/weather/delays")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Airport Delays", description = "FAA airport delay operations")
public class AirportDelayResource {

    @Inject
    AirportDelayService airportDelayService;

    @Inject
    MeterRegistry meterRegistry;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/active")
    @Operation(summary = "Get active delays", description = "Retrieve airports with active delays")
    @APIResponse(responseCode = "200", description = "List of airports with active delays")
    public Response getActiveDelays() {
        return Response.ok(airportDelayService.getActiveDelays())
            .cacheControl(cacheControl(60)).build();
    }

    @GET
    @Path("/airport/{code}")
    @Operation(summary = "Get delays by airport", description = "Retrieve delay status for a specific airport")
    @APIResponse(responseCode = "200", description = "Delay status for the specified airport")
    public Response getDelaysByAirport(
            @PathParam("code") @Parameter(description = "Airport code (e.g., KJFK)") String code) {
        return Response.ok(airportDelayService.getDelaysByAirport(code))
            .cacheControl(cacheControl(60)).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh delays", description = "Manually trigger airport delay data refresh")
    @APIResponse(responseCode = "202", description = "Refresh triggered")
    public Response refreshDelays() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "delay").increment();
            airportDelayService.fetchAndStoreDelays();
            return Response.status(Response.Status.ACCEPTED)
                .entity("Airport delay refresh triggered").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh airport delays").build();
        }
    }
}
