package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class PirepResourceTest {

    @Test
    void testGetRecentPireps() {
        given()
        .when()
            .get("/api/weather/pireps/recent")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testRefreshPirepData() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/pireps/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }

    @Test
    void testRecentPirepsReturnsArray() {
        given()
        .when()
            .get("/api/weather/pireps/recent")
        .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void testRecentPirepsHasCacheControl() {
        given()
        .when()
            .get("/api/weather/pireps/recent")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("max-age=60"));
    }
}
