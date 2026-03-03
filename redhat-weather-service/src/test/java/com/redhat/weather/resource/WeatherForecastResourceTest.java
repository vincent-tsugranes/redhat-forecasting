package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class WeatherForecastResourceTest {

    @Test
    void testGetForecastsByLocationReturns200() {
        given()
        .when()
            .get("/api/weather/forecasts/location/1")
        .then()
            .statusCode(200);
    }

    @Test
    void testGetForecastsByCoordinatesReturns200() {
        given()
            .queryParam("lat", 38.9)
            .queryParam("lon", -77.0)
        .when()
            .get("/api/weather/forecasts/coordinates")
        .then()
            .statusCode(200);
    }

    @Test
    void testGetForecastsByCoordinatesMissingLatReturns400() {
        given()
            .queryParam("lon", -77.0)
        .when()
            .get("/api/weather/forecasts/coordinates")
        .then()
            .statusCode(400);
    }

    @Test
    void testGetCurrentForecastReturnsResult() {
        given()
            .queryParam("lat", 38.9)
            .queryParam("lon", -77.0)
        .when()
            .get("/api/weather/forecasts/current")
        .then()
            .statusCode(anyOf(is(200), is(404)));
    }
}
