package com.veccy.rest.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the Veccy REST API server.
 */
public class RestConfig {
    private final int port;
    private final String host;
    private final boolean enableCors;
    private final String[] allowedOrigins;
    private final int maxRequestSize;
    private final boolean enableCompression;
    private final boolean enableMetrics;
    private final String basePath;
    private final Map<String, Object> securityConfig;
    private final long requestTimeoutMs;
    private final boolean enableHttps;
    private final String keystorePath;
    private final String keystorePassword;
    private final int httpsPort;

    private RestConfig(Builder builder) {
        this.port = builder.port;
        this.host = builder.host;
        this.enableCors = builder.enableCors;
        this.allowedOrigins = builder.allowedOrigins;
        this.maxRequestSize = builder.maxRequestSize;
        this.enableCompression = builder.enableCompression;
        this.enableMetrics = builder.enableMetrics;
        this.basePath = builder.basePath;
        this.securityConfig = builder.securityConfig;
        this.requestTimeoutMs = builder.requestTimeoutMs;
        this.enableHttps = builder.enableHttps;
        this.keystorePath = builder.keystorePath;
        this.keystorePassword = builder.keystorePassword;
        this.httpsPort = builder.httpsPort;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public boolean isEnableCors() {
        return enableCors;
    }

    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public boolean isEnableCompression() {
        return enableCompression;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public String getBasePath() {
        return basePath;
    }

    public Map<String, Object> getSecurityConfig() {
        return securityConfig;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public boolean isEnableHttps() {
        return enableHttps;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    /**
     * Create a default configuration for local development.
     */
    public static RestConfig defaultConfig() {
        return new Builder().build();
    }

    /**
     * Check if production mode is enabled.
     */
    public boolean isProductionMode() {
        return (boolean) securityConfig.getOrDefault("productionMode", false);
    }

    /**
     * Create a production-ready configuration.
     */
    public static RestConfig productionConfig() {
        Map<String, Object> prodSecurity = new HashMap<>();
        prodSecurity.put("productionMode", true);
        prodSecurity.put("apiKeyAuth", true);
        prodSecurity.put("apiKeys", new String[]{});  // Must be configured via environment
        prodSecurity.put("rateLimitEnabled", true);
        prodSecurity.put("maxRequestsPerMinute", 60);

        return new Builder()
                .port(8080)
                .host("0.0.0.0")
                .enableCors(true)
                .allowedOrigins(new String[]{})  // Must be explicitly configured in production
                .enableCompression(true)
                .enableMetrics(true)
                .maxRequestSize(10 * 1024 * 1024) // 10MB
                .requestTimeoutMs(30000) // 30 seconds for production
                .securityConfig(prodSecurity)
                .build();
    }

    /**
     * Builder for RestConfig.
     */
    public static class Builder {
        private int port = 7878;
        private String host = "localhost";
        private boolean enableCors = true;
        private String[] allowedOrigins = new String[]{"*"};
        private int maxRequestSize = 5 * 1024 * 1024; // 5MB default
        private boolean enableCompression = true;
        private boolean enableMetrics = false;
        private String basePath = "/api/v1";
        private Map<String, Object> securityConfig = new HashMap<>();
        private long requestTimeoutMs = 60000; // 60 seconds default
        private boolean enableHttps = false;
        private String keystorePath = null;
        private String keystorePassword = null;
        private int httpsPort = 8443;

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder enableCors(boolean enableCors) {
            this.enableCors = enableCors;
            return this;
        }

        public Builder allowedOrigins(String[] allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
            return this;
        }

        public Builder maxRequestSize(int maxRequestSize) {
            this.maxRequestSize = maxRequestSize;
            return this;
        }

        public Builder enableCompression(boolean enableCompression) {
            this.enableCompression = enableCompression;
            return this;
        }

        public Builder enableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }

        public Builder basePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        public Builder securityConfig(Map<String, Object> securityConfig) {
            this.securityConfig = securityConfig;
            return this;
        }

        public Builder requestTimeoutMs(long requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
            return this;
        }

        public Builder enableHttps(boolean enableHttps) {
            this.enableHttps = enableHttps;
            return this;
        }

        public Builder keystorePath(String keystorePath) {
            this.keystorePath = keystorePath;
            return this;
        }

        public Builder keystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public Builder httpsPort(int httpsPort) {
            this.httpsPort = httpsPort;
            return this;
        }

        public RestConfig build() {
            return new RestConfig(this);
        }
    }
}
