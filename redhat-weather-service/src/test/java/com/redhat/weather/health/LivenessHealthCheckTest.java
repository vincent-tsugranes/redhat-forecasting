package com.redhat.weather.health;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class LivenessHealthCheckTest {

    @Test
    void testLivenessEndpointIsUp() {
        given()
            .when().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
            .body("checks.find { it.name == 'weather-service-live' }.status", equalTo("UP"))
            .body("checks.find { it.name == 'weather-service-live' }.data.uptimeSeconds", notNullValue())
            .body("checks.find { it.name == 'weather-service-live' }.data.heapUsedMB", notNullValue());
    }
}
