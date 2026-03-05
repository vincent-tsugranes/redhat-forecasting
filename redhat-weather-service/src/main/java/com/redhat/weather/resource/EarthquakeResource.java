package com.redhat.weather.resource;

import com.redhat.weather.domain.entity.EarthquakeEntity;
import com.redhat.weather.service.EarthquakeService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/weather/earthquakes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Earthquakes", description = "USGS earthquake data operations")
public class EarthquakeResource {

    @Inject
    EarthquakeService earthquakeService;

    @Inject
    MeterRegistry meterRegistry;

    @GET
    @Path("/recent")
    @Operation(summary = "Get recent earthquakes", description = "Retrieve earthquakes from the last 24 hours (M2.5+)")
    @APIResponse(responseCode = "200", description = "List of recent earthquakes")
    public Response getRecentEarthquakes() {
        List<EarthquakeEntity> earthquakes = earthquakeService.getRecentEarthquakes();
        return Response.ok(earthquakes).build();
    }

    @GET
    @Path("/significant")
    @Operation(summary = "Get significant earthquakes", description = "Retrieve significant earthquakes (M5.0+ or significance >= 500)")
    @APIResponse(responseCode = "200", description = "List of significant earthquakes")
    public Response getSignificantEarthquakes() {
        List<EarthquakeEntity> earthquakes = earthquakeService.getSignificantEarthquakes();
        return Response.ok(earthquakes).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh earthquake data", description = "Manually trigger a refresh of earthquake data")
    @APIResponse(responseCode = "202", description = "Refresh triggered")
    public Response refreshEarthquakeData() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "earthquake").increment();
            earthquakeService.fetchAndStoreEarthquakes();
            return Response.status(Response.Status.ACCEPTED)
                .entity("Earthquake data refresh triggered")
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh earthquake data")
                .build();
        }
    }
}
