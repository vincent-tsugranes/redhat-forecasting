package com.redhat.weather.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;
import java.util.List;

/**
 * REST client for FAA TFR API
 * Discovered from the FAA's Nuxt.js TFR application at tfr.faa.gov/tfr3/
 */
@RegisterRestClient(configKey = "faa-tfr-api")
@Path("/tfrapi")
public interface FaaTfrClient {

    @GET
    @Path("/getTfrList")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 2000, jitter = 500,
           retryOn = {WebApplicationException.class, IOException.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
                    delay = 120000, successThreshold = 3)
    List<TfrListEntry> getTfrList();

    class TfrListEntry {
        public String notam_id;
        public String facility;
        public String state;
        public String type;
        public String description;
        public String mod_date;
        public String mod_abs_time;
        public String is_new;
        public String gid;
    }
}
