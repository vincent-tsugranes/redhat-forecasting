package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class SigmetResourceTest {

    @Test
    void testGetActiveSigmets() {
        given()
        .when()
            .get("/api/weather/sigmets/active")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testRefreshSigmetData() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/sigmets/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }

    @Test
    void testActiveSigmetsReturnsArray() {
        given()
        .when()
            .get("/api/weather/sigmets/active")
        .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void testActiveSigmetsHasCacheControl() {
        given()
        .when()
            .get("/api/weather/sigmets/active")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("max-age=120"));
    }
}
