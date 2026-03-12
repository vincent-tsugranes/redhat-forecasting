package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class LightningResourceTest {

    @Test
    void testGetRecentLightning() {
        given()
        .when()
            .get("/api/weather/lightning/recent")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testGetLightningCount() {
        given()
        .when()
            .get("/api/weather/lightning/count")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testRefreshLightningData() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/lightning/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }

    @Test
    void testRecentLightningReturnsArray() {
        given()
        .when()
            .get("/api/weather/lightning/recent")
        .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void testRecentLightningHasCacheControl() {
        given()
        .when()
            .get("/api/weather/lightning/recent")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("max-age=30"));
    }
}
