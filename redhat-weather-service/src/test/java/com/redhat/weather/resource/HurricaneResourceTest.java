package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class HurricaneResourceTest {

    @Test
    void testGetActiveStormsReturns200() {
        given()
        .when()
            .get("/api/weather/hurricanes/active")
        .then()
            .statusCode(200);
    }

    @Test
    void testGetStormByIdNotFound() {
        given()
        .when()
            .get("/api/weather/hurricanes/NONEXISTENT")
        .then()
            .statusCode(404);
    }

    @Test
    void testGetStormTrackNotFound() {
        given()
        .when()
            .get("/api/weather/hurricanes/NONEXISTENT/track")
        .then()
            .statusCode(404);
    }

    @Test
    void testGetStormTrackWithInvalidDateReturns400() {
        given()
            .queryParam("from", "invalid-date")
        .when()
            .get("/api/weather/hurricanes/AL012024/track")
        .then()
            .statusCode(400);
    }

    @Test
    void testRefreshHurricaneData() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/hurricanes/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }
}
