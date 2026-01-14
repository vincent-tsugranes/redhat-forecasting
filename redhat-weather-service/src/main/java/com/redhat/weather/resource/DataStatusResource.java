package com.redhat.weather.resource;

import com.redhat.weather.domain.repository.LocationRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@Path("/api/status")
@Tag(name = "Data Status", description = "Data loading and system status information")
public class DataStatusResource {

    @Inject
    LocationRepository locationRepository;

    @GET
    @Path("/data")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get data loading status", description = "Returns the current status of data loading including airport counts")
    public Map<String, Object> getDataStatus() {
        Map<String, Object> status = new HashMap<>();

        long totalLocations = locationRepository.count();
        long airportCount = locationRepository.count("locationType = ?1", "airport");
        long cityCount = locationRepository.count("locationType = ?1", "city");

        status.put("totalLocations", totalLocations);
        status.put("airports", airportCount);
        status.put("cities", cityCount);
        status.put("airportsLoaded", airportCount > 0);
        status.put("expectedAirports", 9313); // From CSV
        status.put("loadingComplete", airportCount >= 9313);

        if (airportCount > 0) {
            double percentLoaded = (airportCount / 9313.0) * 100;
            status.put("percentLoaded", Math.round(percentLoaded * 100.0) / 100.0);
        } else {
            status.put("percentLoaded", 0.0);
        }

        return status;
    }
}
