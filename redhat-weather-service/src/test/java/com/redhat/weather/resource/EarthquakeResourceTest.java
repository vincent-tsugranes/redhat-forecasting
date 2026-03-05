package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class EarthquakeResourceTest {

    @Test
    void testGetRecentEarthquakes() {
        given()
        .when()
            .get("/api/weather/earthquakes/recent")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testGetSignificantEarthquakes() {
        given()
        .when()
            .get("/api/weather/earthquakes/significant")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testRefreshEarthquakeData() {
        // Refresh may succeed or fail depending on external API availability,
        // but the endpoint itself should respond with 202 or 500
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/earthquakes/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }

    @Test
    void testRecentEarthquakesReturnsArray() {
        given()
        .when()
            .get("/api/weather/earthquakes/recent")
        .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void testRecentEarthquakesHasCacheControl() {
        given()
        .when()
            .get("/api/weather/earthquakes/recent")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("max-age=120"));
    }

    @Test
    void testSignificantEarthquakesHasCacheControl() {
        given()
        .when()
            .get("/api/weather/earthquakes/significant")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("max-age=120"));
    }
}
