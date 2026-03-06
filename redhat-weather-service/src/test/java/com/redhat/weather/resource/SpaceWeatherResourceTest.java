package com.redhat.weather.resource;

import com.redhat.weather.service.DataFreshnessService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SpaceWeatherResourceTest {

    @Inject
    DataFreshnessService dataFreshnessService;

    @Test
    void testGetSpaceWeather() {
        // Space weather may return 200 with data or 503 if service is unavailable
        given()
        .when()
            .get("/api/weather/space-weather")
        .then()
            .statusCode(anyOf(is(200), is(503)));
    }

    @Test
    void testGetSpaceWeatherHasCacheControlWhenAvailable() {
        var response = given()
        .when()
            .get("/api/weather/space-weather");

        int statusCode = response.statusCode();
        if (statusCode == 200) {
            response.then()
                .header("Cache-Control", containsString("max-age=300"));
        }
    }

    @Test
    void testGetSpaceWeatherResponseStructure() {
        var response = given()
        .when()
            .get("/api/weather/space-weather");

        if (response.statusCode() == 200) {
            response.then()
                .body("$", hasKey("kpIndex"))
                .body("$", hasKey("kpLevel"))
                .body("$", hasKey("geomagneticStormLevel"))
                .body("$", hasKey("auroraChance"))
                .body("$", hasKey("alerts"))
                .body("$", hasKey("fetchedAt"));
        }
    }

    @Test
    void testGetSpaceWeatherRecordsFreshness() {
        var response = given()
        .when()
            .get("/api/weather/space-weather");

        if (response.statusCode() == 200) {
            // A successful fetch should record freshness
            assertNotNull(dataFreshnessService.getLastSuccess("swpc-space-weather"));
        }
    }
}
