package com.redhat.weather.resource;

import com.redhat.weather.domain.entity.WeatherAlertEntity;
import com.redhat.weather.service.WeatherAlertService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/weather/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Weather Alerts", description = "Weather alerts, watches, and warnings operations")
public class WeatherAlertResource {

    @Inject
    WeatherAlertService weatherAlertService;

    @Inject
    MeterRegistry meterRegistry;

    @GET
    @Path("/active")
    @Operation(summary = "Get active alerts", description = "Retrieve all currently active weather alerts")
    @APIResponse(responseCode = "200", description = "List of active alerts")
    public Response getActiveAlerts() {
        List<WeatherAlertEntity> alerts = weatherAlertService.getActiveAlerts();
        return Response.ok(alerts).build();
    }

    @GET
    @Path("/severity/{severity}")
    @Operation(summary = "Get alerts by severity", description = "Retrieve active alerts filtered by severity level")
    @APIResponse(responseCode = "200", description = "List of alerts matching severity")
    public Response getAlertsBySeverity(
            @PathParam("severity") @Parameter(description = "Severity level (Extreme, Severe, Moderate, Minor)") String severity) {

        List<WeatherAlertEntity> alerts = weatherAlertService.getAlertsBySeverity(severity);
        return Response.ok(alerts).build();
    }

    @POST
    @Path("/refresh")
    @Bulkhead(value = 1, waitingTaskQueue = 0)
    @Operation(summary = "Refresh alert data", description = "Manually trigger a refresh of weather alerts")
    @APIResponse(responseCode = "202", description = "Refresh triggered")
    public Response refreshAlerts() {
        try {
            meterRegistry.counter("weather_api_refresh_total", "type", "alerts").increment();
            weatherAlertService.fetchAndStoreAlerts();

            return Response.status(Response.Status.ACCEPTED)
                .entity("Weather alerts refresh triggered")
                .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh weather alerts")
                .build();
        }
    }
}
