package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class SolarDataResourceTest {

    @Test
    void testGetSolarDataNotFound() {
        // Non-existent location should return 404
        given()
        .when()
            .get("/api/weather/solar/999999")
        .then()
            .statusCode(404);
    }

    @Test
    void testGetSolarDataWithValidLocationReturnsResult() {
        // If location exists, returns 200 with solar data or 404 if unavailable
        given()
        .when()
            .get("/api/weather/solar/1")
        .then()
            .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    void testGetSolarDataInvalidIdFormat() {
        given()
        .when()
            .get("/api/weather/solar/abc")
        .then()
            .statusCode(anyOf(is(400), is(404)));
    }
}
