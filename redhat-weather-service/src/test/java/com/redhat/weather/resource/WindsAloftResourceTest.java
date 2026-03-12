package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class WindsAloftResourceTest {

    @Test
    void testGetLatestWindsAloft() {
        given()
        .when()
            .get("/api/weather/winds-aloft/latest")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testRefreshWindsAloftData() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/winds-aloft/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }

    @Test
    void testLatestWindsAloftReturnsArray() {
        given()
        .when()
            .get("/api/weather/winds-aloft/latest")
        .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void testLatestWindsAloftHasCacheControl() {
        given()
        .when()
            .get("/api/weather/winds-aloft/latest")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("max-age=300"));
    }
}
