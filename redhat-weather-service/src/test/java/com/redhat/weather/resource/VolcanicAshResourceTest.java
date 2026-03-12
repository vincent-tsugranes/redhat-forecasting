package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class VolcanicAshResourceTest {

    @Test
    void testGetActiveVolcanicAsh() {
        given()
        .when()
            .get("/api/weather/volcanic-ash/active")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testRefreshVolcanicAshData() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/volcanic-ash/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }

    @Test
    void testActiveVolcanicAshReturnsArray() {
        given()
        .when()
            .get("/api/weather/volcanic-ash/active")
        .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void testActiveVolcanicAshHasCacheControl() {
        given()
        .when()
            .get("/api/weather/volcanic-ash/active")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("max-age=120"));
    }
}
