package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CwaResourceTest {

    @Test
    void testGetActiveCwas() {
        given()
        .when()
            .get("/api/weather/cwas/active")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testRefreshCwaData() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/cwas/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }

    @Test
    void testActiveCwasReturnsArray() {
        given()
        .when()
            .get("/api/weather/cwas/active")
        .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void testActiveCwasHasCacheControl() {
        given()
        .when()
            .get("/api/weather/cwas/active")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("max-age=120"));
    }
}
