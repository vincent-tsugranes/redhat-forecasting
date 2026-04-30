package com.redhat.weather.resource;

import com.redhat.weather.dto.AstronomicalDTO;
import com.redhat.weather.service.AstronomicalService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Path("/api/weather/astronomical")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Astronomical", description = "Sunrise/sunset, moon phases, and UV index")
public class AstronomicalResource {

    @Inject
    AstronomicalService astronomicalService;

    @GET
    @Operation(summary = "Get astronomical data", description = "Returns sunrise/sunset, moon phases, and UV index for given coordinates")
    public Response getAstronomicalData(
            @QueryParam("lat") double lat,
            @QueryParam("lon") double lon,
            @QueryParam("date") String date) {

        if (date == null || date.isEmpty()) {
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        AstronomicalDTO data = astronomicalService.getAstronomicalData(lat, lon, date);

        CacheControl cc = new CacheControl();
        cc.setMaxAge(3600); // 1 hour cache
        return Response.ok(data).cacheControl(cc).build();
    }
}
