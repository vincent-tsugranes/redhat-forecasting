package com.redhat.weather.exception;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Inject
    MeterRegistry meterRegistry;

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            recordError("WebApplicationException", status);
            return buildResponse(status, wae.getMessage());
        }

        if (exception instanceof BulkheadException) {
            LOG.warn("Bulkhead rejected request: " + exception.getMessage());
            recordError("BulkheadException", 429);
            return buildResponse(429, "Too many refresh requests. Please wait and try again.");
        }

        if (exception instanceof CircuitBreakerOpenException) {
            LOG.warn("Circuit breaker is open: " + exception.getMessage());
            recordError("CircuitBreakerOpenException", 503);
            return buildResponse(503, "Weather data source temporarily unavailable. Please retry later.");
        }

        if (exception instanceof TimeoutException) {
            LOG.warn("Request timed out: " + exception.getMessage());
            recordError("TimeoutException", 504);
            return buildResponse(504, "Request to weather data source timed out. Please retry later.");
        }

        if (exception instanceof IllegalArgumentException) {
            recordError("IllegalArgumentException", 400);
            return buildResponse(400, exception.getMessage());
        }

        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            recordError("NotFoundException", 404);
            return buildResponse(404, "Resource not found");
        }

        LOG.error("Unhandled exception", exception);
        recordError("UnhandledException", 500);
        return buildResponse(500, "An unexpected error occurred");
    }

    private void recordError(String type, int status) {
        if (meterRegistry != null) {
            meterRegistry.counter("weather_api_errors_total", "type", type, "status", String.valueOf(status)).increment();
        }
    }

    private Response buildResponse(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        Response.Status httpStatus = Response.Status.fromStatusCode(status);
        body.put("error", httpStatus != null ? httpStatus.getReasonPhrase() : "Error");
        body.put("message", message != null ? message : "Unknown error");
        body.put("timestamp", LocalDateTime.now().toString());

        return Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(body)
            .build();
    }
}
