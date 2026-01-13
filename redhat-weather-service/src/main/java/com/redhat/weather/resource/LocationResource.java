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

import java.util.List;

@Path("/api/weather/locations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Locations", description = "Location management operations")
public class LocationResource {

    @Inject
    LocationService locationService;

    @GET
    @Operation(summary = "Get all locations", description = "Retrieve all weather monitoring locations")
    @APIResponse(responseCode = "200", description = "List of locations")
    public Response getAllLocations() {
        List<LocationEntity> locations = locationService.getAllLocations();
        return Response.ok(locations).build();
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
    @Operation(summary = "Get locations by type", description = "Retrieve locations by type (city, airport, region)")
    @APIResponse(responseCode = "200", description = "List of locations")
    public Response getLocationsByType(@PathParam("type") @Parameter(description = "Location type") String type) {
        List<LocationEntity> locations = locationService.getLocationsByType(type);
        return Response.ok(locations).build();
    }

    @GET
    @Path("/airports")
    @Operation(summary = "Get all airports", description = "Retrieve all airport locations")
    @APIResponse(responseCode = "200", description = "List of airports")
    public Response getAirports() {
        List<LocationEntity> airports = locationService.getAirportLocations();
        return Response.ok(airports).build();
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
    @Operation(summary = "Search locations by name", description = "Search locations by name (partial match)")
    @APIResponse(responseCode = "200", description = "List of matching locations")
    public Response searchLocations(@QueryParam("name") @Parameter(description = "Location name") String name) {
        if (name == null || name.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Query parameter 'name' is required")
                .build();
        }
        List<LocationEntity> locations = locationService.searchLocationsByName(name);
        return Response.ok(locations).build();
    }

    @POST
    @Operation(summary = "Create location", description = "Create a new location")
    @APIResponse(responseCode = "201", description = "Location created")
    @APIResponse(responseCode = "400", description = "Invalid input")
    public Response createLocation(LocationEntity location) {
        LocationEntity created = locationService.createLocation(location);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update location", description = "Update an existing location")
    @APIResponse(responseCode = "200", description = "Location updated")
    @APIResponse(responseCode = "404", description = "Location not found")
    public Response updateLocation(@PathParam("id") @Parameter(description = "Location ID") Long id, LocationEntity location) {
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
}
