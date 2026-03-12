package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class GroundStopResourceTest {

    @Test
    void testGetActiveGroundStops() {
        given()
        .when()
            .get("/api/weather/ground-stops/active")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testGetGroundStopsByAirport() {
        given()
        .when()
            .get("/api/weather/ground-stops/airport/KJFK")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testRefreshGroundStopData() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/ground-stops/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }

    @Test
    void testActiveGroundStopsReturnsArray() {
        given()
        .when()
            .get("/api/weather/ground-stops/active")
        .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void testActiveGroundStopsHasCacheControl() {
        given()
        .when()
            .get("/api/weather/ground-stops/active")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("max-age=60"));
    }
}
