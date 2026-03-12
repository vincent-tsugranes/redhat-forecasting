package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class LocationResourceTest {

    @Test
    void testGetAllLocationsPaginated() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 5)
        .when()
            .get("/api/weather/locations")
        .then()
            .statusCode(200)
            .body("page", equalTo(0))
            .body("size", equalTo(5))
            .body("totalElements", greaterThanOrEqualTo(0))
            .body("totalPages", greaterThanOrEqualTo(0))
            .body("data", notNullValue());
    }

    @Test
    void testPageSizeIsClamped() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 999)
        .when()
            .get("/api/weather/locations")
        .then()
            .statusCode(200)
            .body("size", equalTo(200));
    }

    @Test
    void testGetLocationByIdNotFound() {
        given()
        .when()
            .get("/api/weather/locations/999999")
        .then()
            .statusCode(404);
    }

    @Test
    void testGetAirports() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 5)
        .when()
            .get("/api/weather/locations/airports")
        .then()
            .statusCode(200)
            .body("data", notNullValue())
            .body("page", equalTo(0));
    }

    @Test
    void testGetLocationByAirportCodeNotFound() {
        given()
        .when()
            .get("/api/weather/locations/airport/ZZZZ")
        .then()
            .statusCode(404);
    }

    // --- Pagination contract tests ---

    @Test
    void testPaginatedResponseHasExactKeys() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 5)
        .when()
            .get("/api/weather/locations")
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
    void testDataFieldIsAlwaysArray() {
        // Even beyond available pages, data should be an array, not null
        given()
            .queryParam("page", 999)
            .queryParam("size", 5)
        .when()
            .get("/api/weather/locations")
        .then()
            .statusCode(200)
            .body("data", notNullValue())
            .body("data", instanceOf(java.util.List.class));
    }

    @Test
    void testLargePageSizeClampedAndReturns() {
        // Frontend sends size=10000 for airports; MAX_PAGE_SIZE clamps to 200
        given()
            .queryParam("page", 0)
            .queryParam("size", 10000)
        .when()
            .get("/api/weather/locations/airports")
        .then()
            .statusCode(200)
            .body("size", equalTo(200))
            .body("data", notNullValue());
    }
}
