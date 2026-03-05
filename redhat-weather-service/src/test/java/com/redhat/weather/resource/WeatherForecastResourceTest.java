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

    // --- Pagination contract tests ---

    @Test
    void testForecastsByLocationPaginatedResponseStructure() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/weather/forecasts/location/1")
        .then()
            .statusCode(200)
            .body("$", hasKey("data"))
            .body("$", hasKey("page"))
            .body("$", hasKey("size"))
            .body("$", hasKey("totalElements"))
            .body("$", hasKey("totalPages"))
            .body("data", instanceOf(java.util.List.class));
    }

    @Test
    void testForecastsByCoordinatesPaginatedResponseStructure() {
        given()
            .queryParam("lat", 38.9)
            .queryParam("lon", -77.0)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/weather/forecasts/coordinates")
        .then()
            .statusCode(200)
            .body("$", hasKey("data"))
            .body("$", hasKey("page"))
            .body("$", hasKey("size"))
            .body("$", hasKey("totalElements"))
            .body("$", hasKey("totalPages"))
            .body("data", instanceOf(java.util.List.class));
    }

    @Test
    void testForecastDataIsAlwaysArray() {
        // Even for a location with no forecasts
        given()
            .queryParam("page", 0)
            .queryParam("size", 5)
        .when()
            .get("/api/weather/forecasts/location/999999")
        .then()
            .statusCode(200)
            .body("data", notNullValue())
            .body("data", hasSize(0));
    }

    @Test
    void testDefaultPaginationValuesWhenNoParamsProvided() {
        given()
        .when()
            .get("/api/weather/forecasts/location/1")
        .then()
            .statusCode(200)
            .body("page", equalTo(0))
            .body("size", equalTo(50));
    }
}
