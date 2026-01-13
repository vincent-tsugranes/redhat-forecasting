package com.redhat.weather.resource;

import com.redhat.weather.domain.entity.WeatherForecastEntity;
import com.redhat.weather.service.WeatherForecastService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Path("/api/weather/forecasts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Weather Forecasts", description = "Public weather forecast operations")
public class WeatherForecastResource {

    @Inject
    WeatherForecastService weatherForecastService;

    @GET
    @Path("/location/{locationId}")
    @Operation(summary = "Get forecasts for a location", description = "Retrieve all active forecasts for a specific location")
    @APIResponse(responseCode = "200", description = "List of forecasts")
    public Response getForecastsByLocation(
            @PathParam("locationId") @Parameter(description = "Location ID") Long locationId) {
        List<WeatherForecastEntity> forecasts = weatherForecastService.getForecastsByLocation(locationId);
        return Response.ok(forecasts).build();
    }

    @GET
    @Path("/coordinates")
    @Operation(summary = "Get forecasts by coordinates", description = "Retrieve forecasts for specific coordinates and time range")
    @APIResponse(responseCode = "200", description = "List of forecasts")
    @APIResponse(responseCode = "400", description = "Invalid parameters")
    public Response getForecastsByCoordinates(
            @QueryParam("lat") @Parameter(description = "Latitude", required = true) BigDecimal latitude,
            @QueryParam("lon") @Parameter(description = "Longitude", required = true) BigDecimal longitude,
            @QueryParam("from") @Parameter(description = "Start time (ISO-8601)") String from,
            @QueryParam("to") @Parameter(description = "End time (ISO-8601)") String to) {

        if (latitude == null || longitude == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Latitude and longitude are required")
                .build();
        }

        try {
            LocalDateTime fromTime = from != null ?
                LocalDateTime.parse(from, DateTimeFormatter.ISO_DATE_TIME) :
                LocalDateTime.now();

            LocalDateTime toTime = to != null ?
                LocalDateTime.parse(to, DateTimeFormatter.ISO_DATE_TIME) :
                fromTime.plusDays(7);

            List<WeatherForecastEntity> forecasts =
                weatherForecastService.getForecastsByCoordinates(latitude, longitude, fromTime, toTime);

            return Response.ok(forecasts).build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid date format. Use ISO-8601 format (e.g., 2024-01-15T10:00:00)")
                .build();
        }
    }

    @GET
    @Path("/current")
    @Operation(summary = "Get current forecast", description = "Retrieve current forecast for specific coordinates")
    @APIResponse(responseCode = "200", description = "Current forecast")
    @APIResponse(responseCode = "400", description = "Invalid parameters")
    @APIResponse(responseCode = "404", description = "No forecast found")
    public Response getCurrentForecast(
            @QueryParam("lat") @Parameter(description = "Latitude", required = true) BigDecimal latitude,
            @QueryParam("lon") @Parameter(description = "Longitude", required = true) BigDecimal longitude) {

        if (latitude == null || longitude == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Latitude and longitude are required")
                .build();
        }

        List<WeatherForecastEntity> forecasts = weatherForecastService.getCurrentForecast(latitude, longitude);

        if (forecasts.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("No forecast found for the specified coordinates")
                .build();
        }

        return Response.ok(forecasts).build();
    }
}
