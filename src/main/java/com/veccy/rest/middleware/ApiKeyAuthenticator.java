package com.veccy.rest.middleware;

import com.veccy.rest.dto.ApiResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Middleware for API key authentication.
 * Validates API keys provided in the X-API-Key header.
 */
public class ApiKeyAuthenticator implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticator.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final Set<String> validApiKeys;
    private final boolean enabled;

    public ApiKeyAuthenticator(Map<String, Object> securityConfig) {
        this.enabled = (boolean) securityConfig.getOrDefault("apiKeyAuth", false);
        this.validApiKeys = ConcurrentHashMap.newKeySet();

        // Load API keys from configuration
        Object apiKeys = securityConfig.get("apiKeys");
        if (apiKeys instanceof String[]) {
            for (String key : (String[]) apiKeys) {
                if (key != null && !key.trim().isEmpty()) {
                    this.validApiKeys.add(key);
                }
            }
        }

        if (enabled) {
            logger.info("API key authentication enabled with {} valid keys", validApiKeys.size());
            if (validApiKeys.isEmpty()) {
                logger.warn("API key authentication is enabled but no keys are configured!");
            }
        }
    }

    @Override
    public void handle(@NotNull Context ctx) {
        // Skip authentication if disabled
        if (!enabled) {
            return;
        }

        String apiKey = ctx.header(API_KEY_HEADER);

        // Check if API key is provided
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Authentication failed: No API key provided for {} {}",
                ctx.method(), ctx.path());
            ctx.status(401).json(ApiResponse.error("API key required. Provide it in the " + API_KEY_HEADER + " header."));
            return;
        }

        // Validate API key
        if (!validApiKeys.contains(apiKey)) {
            logger.warn("Authentication failed: Invalid API key for {} {} from {}",
                ctx.method(), ctx.path(), ctx.ip());
            ctx.status(401).json(ApiResponse.error("Invalid API key"));
            return;
        }

        // Authentication successful - store in context for logging
        ctx.attribute("authenticated", true);
        logger.debug("Authentication successful for {} {}", ctx.method(), ctx.path());
    }

    /**
     * Add a valid API key at runtime.
     */
    public void addApiKey(String apiKey) {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            validApiKeys.add(apiKey);
            logger.info("Added new API key. Total valid keys: {}", validApiKeys.size());
        }
    }

    /**
     * Remove an API key at runtime.
     */
    public void removeApiKey(String apiKey) {
        if (validApiKeys.remove(apiKey)) {
            logger.info("Removed API key. Total valid keys: {}", validApiKeys.size());
        }
    }

    /**
     * Check if authentication is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the count of valid API keys.
     */
    public int getKeyCount() {
        return validApiKeys.size();
    }
}
