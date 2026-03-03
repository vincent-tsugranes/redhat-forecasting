package com.redhat.weather.exception;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class GlobalExceptionMapperTest {

    @Inject
    GlobalExceptionMapper mapper;

    @Test
    void testBulkheadExceptionMapsTo429() {
        Response response = mapper.toResponse(new BulkheadException("too many"));
        assertEquals(429, response.getStatus());
        assertBodyContains(response, "Too many refresh requests");
    }

    @Test
    void testCircuitBreakerExceptionMapsTo503() {
        Response response = mapper.toResponse(new CircuitBreakerOpenException("open"));
        assertEquals(503, response.getStatus());
        assertBodyContains(response, "temporarily unavailable");
    }

    @Test
    void testTimeoutExceptionMapsTo504() {
        Response response = mapper.toResponse(new TimeoutException("timed out"));
        assertEquals(504, response.getStatus());
        assertBodyContains(response, "timed out");
    }

    @Test
    void testIllegalArgumentExceptionMapsTo400() {
        Response response = mapper.toResponse(new IllegalArgumentException("bad input"));
        assertEquals(400, response.getStatus());
        assertBodyContains(response, "bad input");
    }

    @Test
    void testNotFoundExceptionMapsTo404() {
        Response response = mapper.toResponse(new jakarta.ws.rs.NotFoundException("not found"));
        assertEquals(404, response.getStatus());
        assertBodyContains(response, "not found");
    }

    @Test
    void testWebApplicationExceptionPassesStatusThrough() {
        Response inner = Response.status(409).build();
        Response response = mapper.toResponse(new WebApplicationException("conflict", inner));
        assertEquals(409, response.getStatus());
    }

    @Test
    void testUnknownExceptionMapsTo500() {
        Response response = mapper.toResponse(new RuntimeException("unexpected"));
        assertEquals(500, response.getStatus());
        assertBodyContains(response, "unexpected error");
    }

    @Test
    void testResponseBodyContainsRequiredFields() {
        Response response = mapper.toResponse(new RuntimeException("test"));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("error"));
        assertTrue(body.containsKey("message"));
        assertTrue(body.containsKey("timestamp"));
    }

    private void assertBodyContains(Response response, String expected) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        String message = (String) body.get("message");
        assertTrue(message.toLowerCase().contains(expected.toLowerCase()),
            "Expected body message to contain '" + expected + "' but got: " + message);
    }
}
