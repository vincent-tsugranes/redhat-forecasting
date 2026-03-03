package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
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
    void testCreateAndGetLocation() {
        String body = """
            {
                "name": "Test City",
                "state": "TX",
                "country": "US",
                "latitude": 30.2672,
                "longitude": -97.7431,
                "locationType": "city"
            }
            """;

        int id = given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/weather/locations")
        .then()
            .statusCode(201)
            .body("name", equalTo("Test City"))
            .body("state", equalTo("TX"))
            .body("id", notNullValue())
        .extract()
            .path("id");

        // Verify retrieval
        given()
        .when()
            .get("/api/weather/locations/" + id)
        .then()
            .statusCode(200)
            .body("name", equalTo("Test City"));

        // Cleanup
        given()
        .when()
            .delete("/api/weather/locations/" + id)
        .then()
            .statusCode(204);
    }

    @Test
    void testUpdateLocation() {
        // Create
        String body = """
            {
                "name": "Update Test",
                "state": "CA",
                "country": "US",
                "latitude": 34.0522,
                "longitude": -118.2437,
                "locationType": "city"
            }
            """;

        int id = given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/weather/locations")
        .then()
            .statusCode(201)
        .extract()
            .path("id");

        // Update
        String updateBody = """
            {
                "name": "Updated City",
                "state": "CA",
                "country": "US",
                "latitude": 34.0522,
                "longitude": -118.2437,
                "locationType": "city"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(updateBody)
        .when()
            .put("/api/weather/locations/" + id)
        .then()
            .statusCode(200)
            .body("name", equalTo("Updated City"));

        // Cleanup
        given().delete("/api/weather/locations/" + id);
    }

    @Test
    void testDeleteLocationNotFound() {
        given()
        .when()
            .delete("/api/weather/locations/999999")
        .then()
            .statusCode(404);
    }

    @Test
    void testSearchLocationsRequiresName() {
        given()
        .when()
            .get("/api/weather/locations/search")
        .then()
            .statusCode(400);
    }

    @Test
    void testSearchLocationsWithName() {
        given()
            .queryParam("name", "nonexistent-location-xyz")
        .when()
            .get("/api/weather/locations/search")
        .then()
            .statusCode(200)
            .body("data", hasSize(0));
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

    @Test
    void testGetLocationsByType() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 5)
        .when()
            .get("/api/weather/locations/type/city")
        .then()
            .statusCode(200)
            .body("data", notNullValue());
    }
}
