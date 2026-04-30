package com.redhat.weather.resource;

import com.redhat.weather.domain.entity.AirQualityEntity;
import com.redhat.weather.domain.repository.LocationRepository;
import com.redhat.weather.service.AirQualityService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Path("/api/weather/air-quality")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Air Quality", description = "Air quality index and pollutant data")
public class AirQualityResource {

    @Inject
    AirQualityService airQualityService;

    @Inject
    LocationRepository locationRepository;

    @Inject
    MeterRegistry meterRegistry;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/location/{locationId}")
    @Operation(summary = "Get latest air quality for location",
               description = "Retrieve the latest AQI and pollutant data for a specific location")
    @APIResponse(responseCode = "200", description = "Latest air quality data")
    @APIResponse(responseCode = "404", description = "No air quality data found for location")
    public Response getLatestByLocation(@PathParam("locationId") Long locationId) {
        Optional<AirQualityEntity> result = airQualityService.getLatestByLocation(locationId);
        if (result.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("No air quality data found for location " + locationId)
                .build();
        }
        return Response.ok(result.get()).cacheControl(cacheControl(120)).build();
    }

    @GET
    @Path("/coordinates")
    @Operation(summary = "Get air quality by coordinates",
               description = "Retrieve AQI and pollutant data by latitude and longitude")
    @APIResponse(responseCode = "200", description = "Air quality data for coordinates")
    public Response getByCoordinates(@QueryParam("lat") BigDecimal lat,
                                     @QueryParam("lon") BigDecimal lon) {
        List<AirQualityEntity> results = airQualityService.getByCoordinates(lat, lon);
        return Response.ok(results).cacheControl(cacheControl(120)).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    @Operation(summary = "Refresh air quality data",
               description = "Manually trigger a refresh of air quality data for the first location")
    @APIResponse(responseCode = "202", description = "Refresh triggered")
    public Response refreshAirQuality() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "air-quality").increment();
            var locations = locationRepository.findAirportLocations();
            if (!locations.isEmpty()) {
                airQualityService.fetchAndStoreAirQuality(locations.get(0).id);
            }
            return Response.status(Response.Status.ACCEPTED)
                .entity("Air quality data refresh triggered")
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh air quality data")
                .build();
        }
    }
}
