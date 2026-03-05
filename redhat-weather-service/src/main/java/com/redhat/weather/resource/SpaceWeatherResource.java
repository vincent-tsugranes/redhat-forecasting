package com.redhat.weather.resource;

import com.redhat.weather.dto.SpaceWeatherDTO;
import com.redhat.weather.service.SpaceWeatherService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/weather/space-weather")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Space Weather", description = "NOAA Space Weather Prediction Center data")
public class SpaceWeatherResource {

    @Inject
    SpaceWeatherService spaceWeatherService;

    @GET
    @Operation(summary = "Get space weather data",
               description = "Retrieve current space weather conditions including Kp index, solar wind, and alerts")
    @APIResponse(responseCode = "200", description = "Space weather data")
    @APIResponse(responseCode = "503", description = "Space weather data unavailable")
    public Response getSpaceWeather() {
        return spaceWeatherService.getSpaceWeather()
            .map(dto -> Response.ok(dto).build())
            .orElse(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("Space weather data temporarily unavailable")
                .build());
    }
}
