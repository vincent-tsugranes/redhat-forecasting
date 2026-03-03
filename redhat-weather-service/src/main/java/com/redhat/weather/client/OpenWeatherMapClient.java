package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;

/**
 * REST client for OpenWeatherMap API
 * API Documentation: https://openweathermap.org/api
 */
@RegisterRestClient(configKey = "openweather-api")
@Path("/data/2.5")
@Timeout(5000)
public interface OpenWeatherMapClient {

    @GET
    @Path("/forecast")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 1000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 120000, successThreshold = 3)
    String getForecast(@QueryParam("lat") double lat,
                      @QueryParam("lon") double lon,
                      @QueryParam("appid") String appid,
                      @QueryParam("units") String units);

    @GET
    @Path("/weather")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 1000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 120000, successThreshold = 3)
    String getCurrentWeather(@QueryParam("lat") double lat,
                            @QueryParam("lon") double lon,
                            @QueryParam("appid") String appid,
                            @QueryParam("units") String units);
}
