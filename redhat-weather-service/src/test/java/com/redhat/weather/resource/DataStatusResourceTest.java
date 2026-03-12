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

    @Test
    void testDataStatusIncludesSchedulerList() {
        given()
        .when()
            .get("/api/status/data")
        .then()
            .statusCode(200)
            .body("$", hasKey("schedulers"))
            .body("schedulers", instanceOf(java.util.List.class))
            .body("schedulers.size()", greaterThanOrEqualTo(5));
    }

    @Test
    void testSchedulerListIncludesSpaceWeather() {
        given()
        .when()
            .get("/api/status/data")
        .then()
            .statusCode(200)
            .body("schedulers.name", hasItem("Space Weather"))
            .body("schedulers.source", hasItem("swpc-space-weather"));
    }

    @Test
    void testSchedulerEntriesHaveRequiredFields() {
        given()
        .when()
            .get("/api/status/data")
        .then()
            .statusCode(200)
            .body("schedulers[0]", hasKey("name"))
            .body("schedulers[0]", hasKey("source"))
            .body("schedulers[0]", hasKey("intervalMinutes"))
            .body("schedulers[0]", hasKey("enabled"));
    }
}
