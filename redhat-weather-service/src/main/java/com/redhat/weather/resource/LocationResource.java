package com.redhat.weather.resource;

import com.redhat.weather.domain.entity.LocationEntity;
import com.redhat.weather.service.LocationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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
@Tag(name = "Locations", description = "Location management operations")
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
    @Path("/type/{type}")
    @Operation(summary = "Get locations by type", description = "Retrieve locations by type (city, airport, region) (paginated)")
    @APIResponse(responseCode = "200", description = "Paginated list of locations")
    public Response getLocationsByType(
            @PathParam("type") @Parameter(description = "Location type") String type,
            @QueryParam("page") @DefaultValue("0") @Parameter(description = "Page number (0-based)") int page,
            @QueryParam("size") @DefaultValue("50") @Parameter(description = "Page size (max 200)") int size) {

        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<LocationEntity> locations = locationService.getLocationsByType(type, page, clampedSize);
        long totalElements = locationService.countLocationsByType(type);
        return Response.ok(buildPageResponse(locations, page, clampedSize, totalElements)).build();
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

    @GET
    @Path("/search")
    @Operation(summary = "Search locations by name", description = "Search locations by name (partial match, paginated)")
    @APIResponse(responseCode = "200", description = "Paginated list of matching locations")
    public Response searchLocations(
            @QueryParam("name") @Parameter(description = "Location name") String name,
            @QueryParam("page") @DefaultValue("0") @Parameter(description = "Page number (0-based)") int page,
            @QueryParam("size") @DefaultValue("50") @Parameter(description = "Page size (max 200)") int size) {

        if (name == null || name.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Query parameter 'name' is required")
                .build();
        }
        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<LocationEntity> locations = locationService.searchLocationsByName(name, page, clampedSize);
        long totalElements = locationService.countLocationsByName(name);
        return Response.ok(buildPageResponse(locations, page, clampedSize, totalElements)).build();
    }

    @POST
    @Operation(summary = "Create location", description = "Create a new location")
    @APIResponse(responseCode = "201", description = "Location created")
    @APIResponse(responseCode = "400", description = "Invalid input")
    public Response createLocation(@Valid LocationEntity location) {
        LocationEntity created = locationService.createLocation(location);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update location", description = "Update an existing location")
    @APIResponse(responseCode = "200", description = "Location updated")
    @APIResponse(responseCode = "404", description = "Location not found")
    public Response updateLocation(@PathParam("id") @Parameter(description = "Location ID") Long id, @Valid LocationEntity location) {
        LocationEntity updated = locationService.updateLocation(id, location);
        if (updated != null) {
            return Response.ok(updated).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete location", description = "Delete a location")
    @APIResponse(responseCode = "204", description = "Location deleted")
    @APIResponse(responseCode = "404", description = "Location not found")
    public Response deleteLocation(@PathParam("id") @Parameter(description = "Location ID") Long id) {
        boolean deleted = locationService.deleteLocation(id);
        if (deleted) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
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
