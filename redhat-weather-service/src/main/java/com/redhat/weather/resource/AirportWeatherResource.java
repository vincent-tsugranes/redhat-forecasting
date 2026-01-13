package com.redhat.weather.resource;

import com.redhat.weather.domain.entity.AirportWeatherEntity;
import com.redhat.weather.service.AirportWeatherService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/weather/airports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Airport Weather", description = "Airport weather (METAR/TAF) operations")
public class AirportWeatherResource {

    @Inject
    AirportWeatherService airportWeatherService;

    @GET
    @Path("/{code}")
    @Operation(summary = "Get airport weather", description = "Retrieve weather data for a specific airport by ICAO code")
    @APIResponse(responseCode = "200", description = "List of weather reports")
    @APIResponse(responseCode = "404", description = "Airport not found")
    public Response getAirportWeather(
            @PathParam("code") @Parameter(description = "ICAO airport code (e.g., KJFK)") String code) {

        List<AirportWeatherEntity> weather = airportWeatherService.getAirportWeather(code);

        if (weather.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("No weather data found for airport: " + code)
                .build();
        }

        return Response.ok(weather).build();
    }

    @GET
    @Path("/{code}/metar")
    @Operation(summary = "Get latest METAR", description = "Retrieve the latest METAR for a specific airport")
    @APIResponse(responseCode = "200", description = "METAR data")
    @APIResponse(responseCode = "404", description = "METAR not found")
    public Response getLatestMetar(
            @PathParam("code") @Parameter(description = "ICAO airport code") String code) {

        return airportWeatherService.getLatestMetar(code)
            .map(metar -> Response.ok(metar).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity("No METAR found for airport: " + code)
                .build());
    }

    @GET
    @Path("/{code}/taf")
    @Operation(summary = "Get latest TAF", description = "Retrieve the latest TAF for a specific airport")
    @APIResponse(responseCode = "200", description = "TAF data")
    @APIResponse(responseCode = "404", description = "TAF not found")
    public Response getLatestTaf(
            @PathParam("code") @Parameter(description = "ICAO airport code") String code) {

        return airportWeatherService.getLatestTaf(code)
            .map(taf -> Response.ok(taf).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity("No TAF found for airport: " + code)
                .build());
    }

    @POST
    @Path("/{code}/refresh")
    @Operation(summary = "Refresh airport weather", description = "Manually trigger a refresh of airport weather data")
    @APIResponse(responseCode = "202", description = "Refresh triggered")
    public Response refreshAirportWeather(
            @PathParam("code") @Parameter(description = "ICAO airport code") String code) {

        try {
            airportWeatherService.fetchAndStoreMETAR(code);
            airportWeatherService.fetchAndStoreTAF(code);

            return Response.status(Response.Status.ACCEPTED)
                .entity("Weather refresh triggered for airport: " + code)
                .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to refresh weather for airport: " + code)
                .build();
        }
    }
}
