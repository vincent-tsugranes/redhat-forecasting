package com.redhat.weather.resource;

import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.service.LocationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/api/weather/locations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Locations", description = "Airport location operations")
public class LocationResource {

    private static final int MAX_PAGE_SIZE = 200;

    @Inject
    LocationService locationService;

    @GET
    @Operation(summary = "Get all locations", description = "Retrieve all weather monitoring locations (paginated)")
    @APIResponse(responseCode = "200", description = "Paginated list of locations")
    public Response getAllLocations(
            @QueryParam("page") @DefaultValue("0") @Parameter(description = "Page number (0-based)") int page,
            @QueryParam("size") @DefaultValue("50") @Parameter(description = "Page size (max 200)") int size) {

        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<LocationEntity> locations = locationService.getAllLocations(page, clampedSize);
        long totalElements = locationService.countAllLocations();
        return Response.ok(buildPageResponse(locations, page, clampedSize, totalElements)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get location by ID", description = "Retrieve a specific location by ID")
    @APIResponse(responseCode = "200", description = "Location found")
    @APIResponse(responseCode = "404", description = "Location not found")
    public Response getLocationById(@PathParam("id") @Parameter(description = "Location ID") Long id) {
        return locationService.getLocationById(id)
            .map(location -> Response.ok(location).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/airports")
    @Operation(summary = "Get all airports", description = "Retrieve all airport locations (paginated)")
    @APIResponse(responseCode = "200", description = "Paginated list of airports")
    public Response getAirports(
            @QueryParam("page") @DefaultValue("0") @Parameter(description = "Page number (0-based)") int page,
            @QueryParam("size") @DefaultValue("50") @Parameter(description = "Page size (max 200)") int size) {

        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<LocationEntity> airports = locationService.getAirportLocations(page, clampedSize);
        long totalElements = locationService.countAirportLocations();
        return Response.ok(buildPageResponse(airports, page, clampedSize, totalElements)).build();
    }

    @GET
    @Path("/airport/{code}")
    @Operation(summary = "Get location by airport code", description = "Retrieve location by ICAO airport code")
    @APIResponse(responseCode = "200", description = "Airport found")
    @APIResponse(responseCode = "404", description = "Airport not found")
    public Response getLocationByAirportCode(@PathParam("code") @Parameter(description = "Airport code") String code) {
        return locationService.getLocationByAirportCode(code)
            .map(location -> Response.ok(location).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    private Map<String, Object> buildPageResponse(List<LocationEntity> data, int page, int size, long totalElements) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", totalElements);
        response.put("totalPages", (int) Math.ceil((double) totalElements / size));
        return response;
    }
}
