package com.redhat.weather.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class WeatherAlertResourceTest {

    @Test
    void testGetActiveAlertsReturns200() {
        given()
        .when()
            .get("/api/weather/alerts/active")
        .then()
            .statusCode(200);
    }

    @Test
    void testGetAlertsBySeverityReturns200() {
        given()
        .when()
            .get("/api/weather/alerts/severity/Extreme")
        .then()
            .statusCode(200);
    }

    @Test
    void testRefreshAlerts() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/weather/alerts/refresh")
        .then()
            .statusCode(anyOf(is(202), is(500)));
    }
}
