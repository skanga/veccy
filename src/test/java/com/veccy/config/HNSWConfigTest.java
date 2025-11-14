package com.veccy.config;

import com.veccy.exceptions.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for type-safe HNSWConfig.
 */
class HNSWConfigTest {

    @Test
    void testDefaultConfig() {
        HNSWConfig config = HNSWConfig.defaults();

        assertEquals(16, config.m());
        assertEquals(200, config.efConstruction());
        assertEquals(50, config.efSearch());
        assertEquals(Metric.COSINE, config.metric());
        assertTrue(config.randomSeed().isEmpty());
    }

    @Test
    void testBuilderWithDefaults() {
        HNSWConfig config = HNSWConfig.builder().build();

        assertEquals(16, config.m());
        assertEquals(200, config.efConstruction());
        assertEquals(50, config.efSearch());
        assertEquals(Metric.COSINE, config.metric());
        assertTrue(config.randomSeed().isEmpty());
    }

    @Test
    void testBuilderWithCustomValues() {
        HNSWConfig config = HNSWConfig.builder()
                .m(32)
                .efConstruction(400)
                .efSearch(100)
                .metric(Metric.EUCLIDEAN)
                .randomSeed(42L)
                .build();

        assertEquals(32, config.m());
        assertEquals(400, config.efConstruction());
        assertEquals(100, config.efSearch());
        assertEquals(Metric.EUCLIDEAN, config.metric());
        assertEquals(Optional.of(42L), config.randomSeed());
    }

    @Test
    void testBuilderFluentApi() {
        HNSWConfig config = HNSWConfig.builder()
                .m(20)
                .efConstruction(300)
                .efSearch(75)
                .metric(Metric.DOT_PRODUCT)
                .build();

        assertEquals(20, config.m());
        assertEquals(300, config.efConstruction());
        assertEquals(75, config.efSearch());
        assertEquals(Metric.DOT_PRODUCT, config.metric());
    }

    @Test
    void testValidation_MTooLow() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () ->
                HNSWConfig.builder().m(1).build()
        );
        assertTrue(exception.getMessage().contains("m must be between 2 and 100"));
    }

    @Test
    void testValidation_MTooHigh() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () ->
                HNSWConfig.builder().m(101).build()
        );
        assertTrue(exception.getMessage().contains("m must be between 2 and 100"));
    }

    @Test
    void testValidation_EfConstructionTooLow() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () ->
                HNSWConfig.builder().efConstruction(9).build()
        );
        assertTrue(exception.getMessage().contains("efConstruction must be between 10 and 1000"));
    }

    @Test
    void testValidation_EfConstructionTooHigh() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () ->
                HNSWConfig.builder().efConstruction(1001).build()
        );
        assertTrue(exception.getMessage().contains("efConstruction must be between 10 and 1000"));
    }

    @Test
    void testValidation_EfSearchTooLow() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () ->
                HNSWConfig.builder().efSearch(9).build()
        );
        assertTrue(exception.getMessage().contains("efSearch must be between 10 and 1000"));
    }

    @Test
    void testValidation_EfSearchTooHigh() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () ->
                HNSWConfig.builder().efSearch(1001).build()
        );
        assertTrue(exception.getMessage().contains("efSearch must be between 10 and 1000"));
    }

    @Test
    void testValidation_EfSearchExceedsEfConstruction() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () ->
                HNSWConfig.builder()
                        .efConstruction(100)
                        .efSearch(200)
                        .build()
        );
        assertTrue(exception.getMessage().contains("efSearch"));
        assertTrue(exception.getMessage().contains("should not exceed"));
        assertTrue(exception.getMessage().contains("efConstruction"));
    }

    @Test
    void testValidation_NullMetric() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () ->
                HNSWConfig.builder().metric(null).build()
        );
        assertTrue(exception.getMessage().contains("metric cannot be null"));
    }

    @Test
    void testMetricEnum_ToString() {
        assertEquals("cosine", Metric.COSINE.toString());
        assertEquals("euclidean", Metric.EUCLIDEAN.toString());
        assertEquals("dot_product", Metric.DOT_PRODUCT.toString());
        assertEquals("manhattan", Metric.MANHATTAN.toString());
    }

    @Test
    void testMetricEnum_FromString() {
        assertEquals(Metric.COSINE, Metric.fromString("cosine"));
        assertEquals(Metric.EUCLIDEAN, Metric.fromString("euclidean"));
        assertEquals(Metric.DOT_PRODUCT, Metric.fromString("dot_product"));
        assertEquals(Metric.MANHATTAN, Metric.fromString("manhattan"));
    }

    @Test
    void testMetricEnum_FromStringCaseInsensitive() {
        assertEquals(Metric.COSINE, Metric.fromString("COSINE"));
        assertEquals(Metric.EUCLIDEAN, Metric.fromString("Euclidean"));
        assertEquals(Metric.DOT_PRODUCT, Metric.fromString("DOT_PRODUCT"));
        assertEquals(Metric.MANHATTAN, Metric.fromString("Manhattan"));
    }

    @Test
    void testMetricEnum_FromStringInvalid() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                Metric.fromString("invalid_metric")
        );
        assertTrue(exception.getMessage().contains("Unknown metric"));
    }

    @Test
    void testDirectConstruction() {
        HNSWConfig config = new HNSWConfig(24, 300, 100, Metric.MANHATTAN, Optional.of(123L));

        assertEquals(24, config.m());
        assertEquals(300, config.efConstruction());
        assertEquals(100, config.efSearch());
        assertEquals(Metric.MANHATTAN, config.metric());
        assertEquals(Optional.of(123L), config.randomSeed());
    }

    @Test
    void testDirectConstruction_WithEmptyOptional() {
        HNSWConfig config = new HNSWConfig(16, 200, 50, Metric.COSINE, Optional.empty());

        assertTrue(config.randomSeed().isEmpty());
    }

    @Test
    void testDirectConstruction_WithNull() {
        // The compact constructor converts null to Optional.empty()
        HNSWConfig config = new HNSWConfig(16, 200, 50, Metric.COSINE, null);

        assertTrue(config.randomSeed().isEmpty());
    }

    @Test
    void testRecordEquality() {
        HNSWConfig config1 = HNSWConfig.builder()
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .metric(Metric.COSINE)
                .build();

        HNSWConfig config2 = HNSWConfig.builder()
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .metric(Metric.COSINE)
                .build();

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void testRecordInequality() {
        HNSWConfig config1 = HNSWConfig.builder()
                .m(16)
                .build();

        HNSWConfig config2 = HNSWConfig.builder()
                .m(32)
                .build();

        assertNotEquals(config1, config2);
    }
}
