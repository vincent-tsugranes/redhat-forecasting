package com.redhat.weather.resource;

import com.redhat.weather.domain.entity.WeatherForecastEntity;
import com.redhat.weather.service.WeatherForecastService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.core.CacheControl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/api/weather/forecasts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Weather Forecasts", description = "Public weather forecast operations")
public class WeatherForecastResource {

    @Inject
    WeatherForecastService weatherForecastService;

    private static CacheControl cacheControl(int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSecs);
        return cc;
    }

    @GET
    @Path("/location/{locationId}")
    @Operation(summary = "Get forecasts for a location", description = "Retrieve active forecasts for a specific location (paginated)")
    @APIResponse(responseCode = "200", description = "List of forecasts")
    public Response getForecastsByLocation(
            @PathParam("locationId") @Parameter(description = "Location ID") Long locationId,
            @QueryParam("page") @DefaultValue("0") @Min(0) @Parameter(description = "Page number (0-based)") int page,
            @QueryParam("size") @DefaultValue("50") @Min(1) @Max(200) @Parameter(description = "Page size (max 200)") int size) {
        int clampedSize = Math.min(Math.max(size, 1), 200);
        List<WeatherForecastEntity> forecasts = weatherForecastService.getForecastsByLocation(locationId, page, clampedSize);
        long totalElements = weatherForecastService.countForecastsByLocation(locationId);
        return Response.ok(buildPageResponse(forecasts, page, clampedSize, totalElements))
                .cacheControl(cacheControl(300)).build();
    }

    @GET
    @Path("/coordinates")
    @Operation(summary = "Get forecasts by coordinates", description = "Retrieve forecasts for specific coordinates and time range")
    @APIResponse(responseCode = "200", description = "List of forecasts")
    @APIResponse(responseCode = "400", description = "Invalid parameters")
    public Response getForecastsByCoordinates(
            @QueryParam("lat") @NotNull @DecimalMin("-90") @DecimalMax("90") @Parameter(description = "Latitude", required = true) BigDecimal latitude,
            @QueryParam("lon") @NotNull @DecimalMin("-180") @DecimalMax("180") @Parameter(description = "Longitude", required = true) BigDecimal longitude,
            @QueryParam("from") @Parameter(description = "Start time (ISO-8601)") String from,
            @QueryParam("to") @Parameter(description = "End time (ISO-8601)") String to,
            @QueryParam("page") @DefaultValue("0") @Min(0) @Parameter(description = "Page number (0-based)") int page,
            @QueryParam("size") @DefaultValue("50") @Min(1) @Max(200) @Parameter(description = "Page size (max 200)") int size) {

        try {
            LocalDateTime fromTime = from != null ?
                LocalDateTime.parse(from, DateTimeFormatter.ISO_DATE_TIME) :
                LocalDateTime.now();

            LocalDateTime toTime = to != null ?
                LocalDateTime.parse(to, DateTimeFormatter.ISO_DATE_TIME) :
                fromTime.plusDays(7);

            int clampedSize = Math.min(Math.max(size, 1), 200);
            List<WeatherForecastEntity> forecasts =
                weatherForecastService.getForecastsByCoordinates(latitude, longitude, fromTime, toTime, page, clampedSize);
            long totalElements = weatherForecastService.countForecastsByCoordinatesAndTimeRange(latitude, longitude, fromTime, toTime);

            return Response.ok(buildPageResponse(forecasts, page, clampedSize, totalElements))
                    .cacheControl(cacheControl(300)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid date format. Use ISO-8601 format (e.g., 2024-01-15T10:00:00)")
                .build();
        }
    }

    @GET
    @Path("/location/{locationId}/history")
    @Operation(summary = "Get historical forecasts", description = "Retrieve archived/historical forecast data for a location (paginated)")
    @APIResponse(responseCode = "200", description = "List of historical forecasts")
    public Response getHistoricalForecasts(
            @PathParam("locationId") @Parameter(description = "Location ID") Long locationId,
            @QueryParam("days") @DefaultValue("7") @Min(1) @Max(90) @Parameter(description = "Number of days of history") int days,
            @QueryParam("page") @DefaultValue("0") @Min(0) @Parameter(description = "Page number (0-based)") int page,
            @QueryParam("size") @DefaultValue("50") @Min(1) @Max(200) @Parameter(description = "Page size (max 200)") int size) {
        int clampedSize = Math.min(Math.max(size, 1), 200);
        List<WeatherForecastEntity> forecasts = weatherForecastService.getHistoricalForecasts(locationId, days, page, clampedSize);
        long totalElements = weatherForecastService.countHistoricalForecasts(locationId, days);
        return Response.ok(buildPageResponse(forecasts, page, clampedSize, totalElements))
                .cacheControl(cacheControl(300)).build();
    }

    @GET
    @Path("/current")
    @Operation(summary = "Get current forecast", description = "Retrieve current forecast for specific coordinates")
    @APIResponse(responseCode = "200", description = "Current forecast")
    @APIResponse(responseCode = "400", description = "Invalid parameters")
    @APIResponse(responseCode = "404", description = "No forecast found")
    public Response getCurrentForecast(
            @QueryParam("lat") @NotNull @DecimalMin("-90") @DecimalMax("90") @Parameter(description = "Latitude", required = true) BigDecimal latitude,
            @QueryParam("lon") @NotNull @DecimalMin("-180") @DecimalMax("180") @Parameter(description = "Longitude", required = true) BigDecimal longitude) {

        List<WeatherForecastEntity> forecasts = weatherForecastService.getCurrentForecast(latitude, longitude);

        if (forecasts.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("No forecast found for the specified coordinates")
                .build();
        }

        return Response.ok(forecasts).cacheControl(cacheControl(300)).build();
    }

    private Map<String, Object> buildPageResponse(List<WeatherForecastEntity> data, int page, int size, long totalElements) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", totalElements);
        response.put("totalPages", (int) Math.ceil((double) totalElements / size));
        return response;
    }
}
