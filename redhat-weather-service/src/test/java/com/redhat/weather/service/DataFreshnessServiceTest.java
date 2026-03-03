package com.redhat.weather.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DataFreshnessServiceTest {

    @Inject
    DataFreshnessService dataFreshnessService;

    @Test
    void testRecordAndGetSuccess() {
        dataFreshnessService.recordSuccess("test-source");

        LocalDateTime lastSuccess = dataFreshnessService.getLastSuccess("test-source");
        assertNotNull(lastSuccess);
        assertTrue(lastSuccess.isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(lastSuccess.isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    @Test
    void testGetLastSuccessReturnsNullForUnknownSource() {
        assertNull(dataFreshnessService.getLastSuccess("nonexistent-source-" + System.nanoTime()));
    }

    @Test
    void testGetFreshnessSnapshotContainsRecordedSources() {
        String source = "snapshot-test-" + System.nanoTime();
        dataFreshnessService.recordSuccess(source);

        Map<String, Object> snapshot = dataFreshnessService.getFreshnessSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.containsKey(source + ".lastSuccess"));
        assertTrue(snapshot.containsKey(source + ".ageMinutes"));

        long ageMinutes = (long) snapshot.get(source + ".ageMinutes");
        assertTrue(ageMinutes >= 0 && ageMinutes < 1);
    }

    @Test
    void testMultipleSourcesTrackedIndependently() {
        String source1 = "source-a-" + System.nanoTime();
        String source2 = "source-b-" + System.nanoTime();

        dataFreshnessService.recordSuccess(source1);
        dataFreshnessService.recordSuccess(source2);

        assertNotNull(dataFreshnessService.getLastSuccess(source1));
        assertNotNull(dataFreshnessService.getLastSuccess(source2));
    }
}
