package com.veccy.rest.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for RestConfig.
 */
class RestConfigTest {

    @Test
    void testDefaultConfig() {
        RestConfig config = RestConfig.defaultConfig();

        assertEquals(7878, config.getPort());
        assertEquals("localhost", config.getHost());
        assertTrue(config.isEnableCors());
        assertNotNull(config.getAllowedOrigins());
        assertEquals(1, config.getAllowedOrigins().length);
        assertEquals("*", config.getAllowedOrigins()[0]);
        assertEquals(5 * 1024 * 1024, config.getMaxRequestSize());
        assertTrue(config.isEnableCompression());
        assertFalse(config.isEnableMetrics());
        assertEquals("/api/v1", config.getBasePath());
        assertNotNull(config.getSecurityConfig());
        assertEquals(60000, config.getRequestTimeoutMs());
        assertFalse(config.isEnableHttps());
        assertNull(config.getKeystorePath());
        assertNull(config.getKeystorePassword());
        assertEquals(8443, config.getHttpsPort());
        assertFalse(config.isProductionMode());
    }

    @Test
    void testProductionConfig() {
        RestConfig config = RestConfig.productionConfig();

        assertEquals(8080, config.getPort());
        assertEquals("0.0.0.0", config.getHost());
        assertTrue(config.isEnableCors());
        assertEquals(0, config.getAllowedOrigins().length);
        assertTrue(config.isEnableCompression());
        assertTrue(config.isEnableMetrics());
        assertEquals(10 * 1024 * 1024, config.getMaxRequestSize());
        assertEquals(30000, config.getRequestTimeoutMs());
        assertTrue(config.isProductionMode());
        assertTrue((Boolean) config.getSecurityConfig().get("apiKeyAuth"));
        assertTrue((Boolean) config.getSecurityConfig().get("rateLimitEnabled"));
        assertEquals(60, config.getSecurityConfig().get("maxRequestsPerMinute"));
    }

    @Test
    void testBuilderPort() {
        RestConfig config = new RestConfig.Builder()
                .port(9000)
                .build();

        assertEquals(9000, config.getPort());
    }

    @Test
    void testBuilderHost() {
        RestConfig config = new RestConfig.Builder()
                .host("192.168.1.1")
                .build();

        assertEquals("192.168.1.1", config.getHost());
    }

    @Test
    void testBuilderEnableCors() {
        RestConfig config = new RestConfig.Builder()
                .enableCors(false)
                .build();

        assertFalse(config.isEnableCors());
    }

    @Test
    void testBuilderAllowedOrigins() {
        String[] origins = {"http://localhost:3000", "https://example.com"};
        RestConfig config = new RestConfig.Builder()
                .allowedOrigins(origins)
                .build();

        assertArrayEquals(origins, config.getAllowedOrigins());
    }

    @Test
    void testBuilderMaxRequestSize() {
        RestConfig config = new RestConfig.Builder()
                .maxRequestSize(1024 * 1024)
                .build();

        assertEquals(1024 * 1024, config.getMaxRequestSize());
    }

    @Test
    void testBuilderEnableCompression() {
        RestConfig config = new RestConfig.Builder()
                .enableCompression(false)
                .build();

        assertFalse(config.isEnableCompression());
    }

    @Test
    void testBuilderEnableMetrics() {
        RestConfig config = new RestConfig.Builder()
                .enableMetrics(true)
                .build();

        assertTrue(config.isEnableMetrics());
    }

    @Test
    void testBuilderBasePath() {
        RestConfig config = new RestConfig.Builder()
                .basePath("/custom/api")
                .build();

        assertEquals("/custom/api", config.getBasePath());
    }

    @Test
    void testBuilderSecurityConfig() {
        Map<String, Object> security = new HashMap<>();
        security.put("apiKey", "test-key");

        RestConfig config = new RestConfig.Builder()
                .securityConfig(security)
                .build();

        assertEquals(security, config.getSecurityConfig());
        assertEquals("test-key", config.getSecurityConfig().get("apiKey"));
    }

    @Test
    void testBuilderRequestTimeoutMs() {
        RestConfig config = new RestConfig.Builder()
                .requestTimeoutMs(5000)
                .build();

        assertEquals(5000, config.getRequestTimeoutMs());
    }

    @Test
    void testBuilderEnableHttps() {
        RestConfig config = new RestConfig.Builder()
                .enableHttps(true)
                .build();

        assertTrue(config.isEnableHttps());
    }

    @Test
    void testBuilderKeystorePath() {
        RestConfig config = new RestConfig.Builder()
                .keystorePath("/path/to/keystore.jks")
                .build();

        assertEquals("/path/to/keystore.jks", config.getKeystorePath());
    }

    @Test
    void testBuilderKeystorePassword() {
        RestConfig config = new RestConfig.Builder()
                .keystorePassword("secret")
                .build();

        assertEquals("secret", config.getKeystorePassword());
    }

    @Test
    void testBuilderHttpsPort() {
        RestConfig config = new RestConfig.Builder()
                .httpsPort(9443)
                .build();

        assertEquals(9443, config.getHttpsPort());
    }

    @Test
    void testBuilderChaining() {
        RestConfig config = new RestConfig.Builder()
                .port(8000)
                .host("0.0.0.0")
                .enableCors(true)
                .allowedOrigins(new String[]{"*"})
                .maxRequestSize(2048)
                .enableCompression(true)
                .enableMetrics(true)
                .basePath("/v2")
                .requestTimeoutMs(10000)
                .enableHttps(true)
                .keystorePath("/keystore")
                .keystorePassword("pass")
                .httpsPort(8443)
                .build();

        assertEquals(8000, config.getPort());
        assertEquals("0.0.0.0", config.getHost());
        assertTrue(config.isEnableCors());
        assertEquals(2048, config.getMaxRequestSize());
        assertTrue(config.isEnableMetrics());
        assertEquals("/v2", config.getBasePath());
        assertEquals(10000, config.getRequestTimeoutMs());
        assertTrue(config.isEnableHttps());
        assertEquals("/keystore", config.getKeystorePath());
        assertEquals("pass", config.getKeystorePassword());
        assertEquals(8443, config.getHttpsPort());
    }

    @Test
    void testIsProductionModeTrue() {
        Map<String, Object> security = new HashMap<>();
        security.put("productionMode", true);

        RestConfig config = new RestConfig.Builder()
                .securityConfig(security)
                .build();

        assertTrue(config.isProductionMode());
    }

    @Test
    void testIsProductionModeFalse() {
        Map<String, Object> security = new HashMap<>();
        security.put("productionMode", false);

        RestConfig config = new RestConfig.Builder()
                .securityConfig(security)
                .build();

        assertFalse(config.isProductionMode());
    }

    @Test
    void testIsProductionModeDefault() {
        RestConfig config = new RestConfig.Builder().build();

        assertFalse(config.isProductionMode());
    }

    @Test
    void testGetAllMethodsWork() {
        Map<String, Object> security = new HashMap<>();
        security.put("key", "value");

        RestConfig config = new RestConfig.Builder()
                .port(1234)
                .host("host")
                .enableCors(true)
                .allowedOrigins(new String[]{"origin"})
                .maxRequestSize(999)
                .enableCompression(true)
                .enableMetrics(true)
                .basePath("/path")
                .securityConfig(security)
                .requestTimeoutMs(123)
                .enableHttps(true)
                .keystorePath("path")
                .keystorePassword("pwd")
                .httpsPort(443)
                .build();

        // Test all getters
        assertEquals(1234, config.getPort());
        assertEquals("host", config.getHost());
        assertTrue(config.isEnableCors());
        assertEquals(1, config.getAllowedOrigins().length);
        assertEquals(999, config.getMaxRequestSize());
        assertTrue(config.isEnableCompression());
        assertTrue(config.isEnableMetrics());
        assertEquals("/path", config.getBasePath());
        assertEquals(security, config.getSecurityConfig());
        assertEquals(123, config.getRequestTimeoutMs());
        assertTrue(config.isEnableHttps());
        assertEquals("path", config.getKeystorePath());
        assertEquals("pwd", config.getKeystorePassword());
        assertEquals(443, config.getHttpsPort());
    }

    @Test
    void testDefaultValues() {
        RestConfig config = new RestConfig.Builder().build();

        assertEquals(7878, config.getPort());
        assertEquals("localhost", config.getHost());
        assertTrue(config.isEnableCors());
        assertTrue(config.isEnableCompression());
        assertFalse(config.isEnableMetrics());
        assertEquals("/api/v1", config.getBasePath());
        assertEquals(60000, config.getRequestTimeoutMs());
        assertFalse(config.isEnableHttps());
        assertEquals(8443, config.getHttpsPort());
    }

    @Test
    void testEmptySecurityConfig() {
        RestConfig config = new RestConfig.Builder()
                .securityConfig(new HashMap<>())
                .build();

        assertNotNull(config.getSecurityConfig());
        assertTrue(config.getSecurityConfig().isEmpty());
        assertFalse(config.isProductionMode());
    }

    @Test
    void testProductionConfigSecuritySettings() {
        RestConfig config = RestConfig.productionConfig();

        Map<String, Object> security = config.getSecurityConfig();
        assertTrue((Boolean) security.get("productionMode"));
        assertTrue((Boolean) security.get("apiKeyAuth"));
        assertNotNull(security.get("apiKeys"));
        assertTrue((Boolean) security.get("rateLimitEnabled"));
        assertEquals(60, security.get("maxRequestsPerMinute"));
    }
}
