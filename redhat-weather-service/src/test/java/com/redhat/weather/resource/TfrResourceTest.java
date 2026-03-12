package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class TfrResourceTest {

    @Test
    void testGetActiveTfrs() {
        given()
        .when()
            .get("/api/weather/tfrs/active")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testRefreshTfrData() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/tfrs/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }

    @Test
    void testActiveTfrsReturnsArray() {
        given()
        .when()
            .get("/api/weather/tfrs/active")
        .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void testActiveTfrsHasCacheControl() {
        given()
        .when()
            .get("/api/weather/tfrs/active")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("max-age=120"));
    }
}
