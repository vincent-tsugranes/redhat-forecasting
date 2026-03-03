package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AirportWeatherResourceTest {

    @Test
    void testGetAirportWeatherNotFound() {
        given()
        .when()
            .get("/api/weather/airports/ZZZZ")
        .then()
            .statusCode(404);
    }

    @Test
    void testGetLatestMetarNotFound() {
        given()
        .when()
            .get("/api/weather/airports/ZZZZ/metar")
        .then()
            .statusCode(404);
    }

    @Test
    void testGetLatestTafNotFound() {
        given()
        .when()
            .get("/api/weather/airports/ZZZZ/taf")
        .then()
            .statusCode(404);
    }

    @Test
    void testRefreshAirportWeather() {
        // Refresh may succeed or fail depending on external API availability,
        // but the endpoint itself should respond with 202 or 500
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/airports/KJFK/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }
}
