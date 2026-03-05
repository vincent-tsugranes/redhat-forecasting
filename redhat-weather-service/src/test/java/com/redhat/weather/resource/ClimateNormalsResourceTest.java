package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ClimateNormalsResourceTest {

    @Test
    void testGetClimateNormalsNotFound() {
        // Non-existent location should return 404
        given()
        .when()
            .get("/api/weather/climate/999999")
        .then()
            .statusCode(404);
    }

    @Test
    void testGetClimateNormalsWithValidLocationReturnsResult() {
        // If location exists but has no historical data, expect 404
        // If location has data, expect 200
        given()
        .when()
            .get("/api/weather/climate/1")
        .then()
            .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    void testGetClimateNormalsInvalidIdFormat() {
        given()
        .when()
            .get("/api/weather/climate/abc")
        .then()
            .statusCode(anyOf(is(400), is(404)));
    }
}
