package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class SpaceWeatherResourceTest {

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
}
