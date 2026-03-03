package com.redhat.weather.exception;

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

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            return buildResponse(status, wae.getMessage());
        }

        if (exception instanceof BulkheadException) {
            LOG.warn("Bulkhead rejected request: " + exception.getMessage());
            return buildResponse(429, "Too many refresh requests. Please wait and try again.");
        }

        if (exception instanceof CircuitBreakerOpenException) {
            LOG.warn("Circuit breaker is open: " + exception.getMessage());
            return buildResponse(503, "Weather data source temporarily unavailable. Please retry later.");
        }

        if (exception instanceof TimeoutException) {
            LOG.warn("Request timed out: " + exception.getMessage());
            return buildResponse(504, "Request to weather data source timed out. Please retry later.");
        }

        if (exception instanceof IllegalArgumentException) {
            return buildResponse(400, exception.getMessage());
        }

        if (exception instanceof jakarta.ws.rs.NotFoundException) {
            return buildResponse(404, "Resource not found");
        }

        LOG.error("Unhandled exception", exception);
        return buildResponse(500, "An unexpected error occurred");
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
