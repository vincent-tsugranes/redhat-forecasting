package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class DataStatusResourceTest {

    @Test
    void testGetDataStatusReturns200WithExpectedKeys() {
        given()
        .when()
            .get("/api/status/data")
        .then()
            .statusCode(200)
            .body("totalLocations", notNullValue())
            .body("airports", notNullValue())
            .body("cities", notNullValue())
            .body("airportsLoaded", notNullValue())
            .body("expectedAirports", equalTo(9313))
            .body("percentLoaded", notNullValue())
            .body("dataFreshness", notNullValue());
    }

    @Test
    void testDataStatusIncludesActiveDataKeys() {
        given()
        .when()
            .get("/api/status/data")
        .then()
            .statusCode(200)
            .body("$", hasKey("activeForecasts"))
            .body("$", hasKey("activeEarthquakes"))
            .body("$", hasKey("activeHurricanes"))
            .body("$", hasKey("metarReports"))
            .body("activeForecasts", notNullValue())
            .body("activeEarthquakes", notNullValue())
            .body("activeHurricanes", notNullValue())
            .body("metarReports", notNullValue());
    }

    @Test
    void testDataFreshnessIsNotNull() {
        given()
        .when()
            .get("/api/status/data")
        .then()
            .statusCode(200)
            .body("dataFreshness", notNullValue())
            .body("dataFreshness", instanceOf(java.util.Map.class));
    }
}
