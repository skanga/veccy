package com.veccy.rest;

import com.veccy.rest.config.RestConfig;
import com.veccy.rest.config.ServerContext;
import com.veccy.rest.handlers.*;
import com.veccy.rest.middleware.VeccyExceptionHandler;
import com.veccy.rest.middleware.RequestLogger;
import com.veccy.rest.middleware.ContentTypeValidator;
import com.veccy.rest.middleware.ApiKeyAuthenticator;
import com.veccy.rest.middleware.RateLimiter;
import com.veccy.rest.middleware.ApiVersionInterceptor;
import com.veccy.rest.middleware.RequestTimeoutHandler;
import com.veccy.rest.middleware.MetricsMiddleware;
import com.veccy.rest.metrics.MetricsCollector;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.OpenApiPluginConfiguration;
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main entry point for the Veccy REST API server.
 * Provides a RESTful interface for vector database operations.
 */
public class VeccyRestServer {
    private static final Logger logger = LoggerFactory.getLogger(VeccyRestServer.class);

    private final Javalin app;
    private final ServerContext context;
    private final RestConfig config;
    private final long startTime;
    private final ApiKeyAuthenticator authenticator;
    private final RateLimiter rateLimiter;
    private final RequestTimeoutHandler timeoutHandler;
    private final MetricsCollector metricsCollector;
    private final MetricsMiddleware metricsMiddleware;

    public VeccyRestServer(RestConfig config) {
        this.config = config;
        this.startTime = System.currentTimeMillis();
        this.context = new ServerContext(config);
        this.authenticator = new ApiKeyAuthenticator(config.getSecurityConfig());
        this.rateLimiter = new RateLimiter(config.getSecurityConfig());
        this.timeoutHandler = new RequestTimeoutHandler(config.getRequestTimeoutMs());
        this.metricsCollector = new MetricsCollector();
        this.metricsMiddleware = new MetricsMiddleware(metricsCollector);
        this.app = createApp();
        setupRoutes();
    }

    public VeccyRestServer() {
        this(RestConfig.defaultConfig());
    }

    private Javalin createApp() {
        Javalin app = Javalin.create(cfg -> {
            // CORS configuration
            if (config.isEnableCors()) {
                cfg.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
                        String[] allowedOrigins = config.getAllowedOrigins();

                        // Production mode: Require explicit origins
                        if (config.isProductionMode()) {
                            if (allowedOrigins.length == 0) {
                                logger.warn("CORS enabled in production mode but no allowed origins configured!");
                            } else {
                                logger.info("CORS enabled for {} origins in production mode", allowedOrigins.length);
                                for (String origin : allowedOrigins) {
                                    if ("*".equals(origin)) {
                                        logger.error("Wildcard CORS origin (*) not allowed in production mode!");
                                    } else {
                                        it.allowHost(origin);
                                    }
                                }
                            }
                        } else {
                            // Development mode: Allow wildcard
                            boolean hasWildcard = false;
                            for (String origin : allowedOrigins) {
                                if ("*".equals(origin)) {
                                    hasWildcard = true;
                                    break;
                                }
                            }

                            if (hasWildcard) {
                                it.anyHost();
                                logger.info("CORS enabled for all origins (development mode)");
                            } else {
                                for (String origin : allowedOrigins) {
                                    it.allowHost(origin);
                                }
                                logger.info("CORS enabled for {} specific origins", allowedOrigins.length);
                            }
                        }
                    });
                });
            }

            // Compression - Javalin 6 handles this automatically via Accept-Encoding
            // No explicit configuration needed

            // HTTP configuration
            cfg.http.prefer405over404 = true;
            cfg.http.maxRequestSize = config.getMaxRequestSize();
            cfg.http.asyncTimeout = config.getRequestTimeoutMs();

            // HTTPS/TLS configuration
            if (config.isEnableHttps()) {
                if (config.getKeystorePath() != null && config.getKeystorePassword() != null) {
                    logger.info("Configuring HTTPS with keystore: {}", config.getKeystorePath());
                    cfg.jetty.addConnector((server, httpConfig) -> {
                        var sslContextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory.Server();
                        sslContextFactory.setKeyStorePath(config.getKeystorePath());
                        sslContextFactory.setKeyStorePassword(config.getKeystorePassword());

                        var httpsConfig = new org.eclipse.jetty.server.HttpConfiguration();
                        httpsConfig.setSecureScheme("https");
                        httpsConfig.setSecurePort(config.getHttpsPort());
                        httpsConfig.addCustomizer(new org.eclipse.jetty.server.SecureRequestCustomizer());

                        var https = new org.eclipse.jetty.server.ServerConnector(
                            server,
                            new org.eclipse.jetty.server.SslConnectionFactory(sslContextFactory, "http/1.1"),
                            new org.eclipse.jetty.server.HttpConnectionFactory(httpsConfig)
                        );
                        https.setPort(config.getHttpsPort());

                        return https;
                    });
                } else {
                    logger.warn("HTTPS enabled but keystore path or password not configured. HTTPS will not be available.");
                }
            }

            // OpenAPI/Swagger documentation
            cfg.registerPlugin(new OpenApiPlugin(pluginConfig -> {
                pluginConfig.withDefinitionConfiguration((version, definition) -> {
                    definition.withInfo(info -> {
                        info.setTitle("Veccy Vector Database API");
                        info.setVersion("1.0.0");
                        info.setDescription("RESTful API for managing vector databases with Veccy");
                    });
                });
            }));

            cfg.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
                swaggerConfig.setDocumentationPath("/swagger-docs");
                swaggerConfig.setUiPath("/swagger-ui");
            }));
        });

        // Add middleware in order: versioning -> metrics -> timeout -> rate limiting -> auth -> logging -> validation
        // API versioning (adds version headers to all responses)
        app.before(new ApiVersionInterceptor());

        // Metrics collection (before handler to track start time)
        if (config.isEnableMetrics()) {
            app.before(metricsMiddleware);
            app.after(ctx -> metricsMiddleware.recordAfter(ctx));
        }

        // Request timeout tracking
        app.before(timeoutHandler);

        // Rate limiting applies first to reject excessive requests early
        app.before(rateLimiter);

        // Authentication applies to /api/* routes only
        app.before("/api/*", authenticator);

        // Request logging
        app.before(new RequestLogger());

        // Content-Type validation for /api/* routes
        app.before("/api/*", new ContentTypeValidator());

        // Global exception handler with production mode
        app.exception(Exception.class, new VeccyExceptionHandler(config.isProductionMode()));

        return app;
    }

    private void setupRoutes() {
        String basePath = config.getBasePath();

        // Health check endpoint
        app.get("/health", ctx -> {
            var result = context.getHealthCheckRegistry().runHealthChecks();
            ctx.status(result.isHealthy() ? 200 : 503);
            ctx.json(result.toMap());
        });

        // Root endpoint
        app.get("/", this::handleRoot);

        // Database management endpoints
        DatabaseHandler dbHandler = new DatabaseHandler(context);
        app.post(basePath + "/databases", dbHandler::createDatabase);
        app.get(basePath + "/databases", dbHandler::listDatabases);
        app.get(basePath + "/databases/{name}", dbHandler::getDatabase);
        app.delete(basePath + "/databases/{name}", dbHandler::deleteDatabase);
        app.get(basePath + "/databases/{name}/stats", dbHandler::getDatabaseStats);

        // Vector CRUD endpoints
        VectorHandler vectorHandler = new VectorHandler(context);
        app.post(basePath + "/databases/{name}/vectors", vectorHandler::insertVectors);
        app.get(basePath + "/databases/{name}/vectors/search", vectorHandler::searchVectors);
        app.get(basePath + "/databases/{name}/vectors/{id}", vectorHandler::getVector);
        app.put(basePath + "/databases/{name}/vectors/{id}", vectorHandler::updateVector);
        app.delete(basePath + "/databases/{name}/vectors/{id}", vectorHandler::deleteVector);
        app.get(basePath + "/databases/{name}/vectors", vectorHandler::listVectors);

        // Batch operations
        BatchHandler batchHandler = new BatchHandler(context);
        app.post(basePath + "/databases/{name}/vectors/batch", batchHandler::batchInsert);
        app.post(basePath + "/databases/{name}/vectors/batch-search", batchHandler::batchSearch);
        app.delete(basePath + "/databases/{name}/vectors/batch", batchHandler::batchDelete);

        // Import/Export endpoints
        ImportExportHandler ieHandler = new ImportExportHandler(context);
        app.post(basePath + "/databases/{name}/import", ieHandler::importData);
        app.get(basePath + "/databases/{name}/export", ieHandler::exportData);

        // Metrics endpoint (if enabled)
        if (config.isEnableMetrics()) {
            app.get(basePath + "/metrics", this::handleMetrics);
        }
    }

    private void handleRoot(Context ctx) {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Veccy Vector Database");
        info.put("version", "1.0.0");
        info.put("api_version", "v1");
        info.put("endpoints", Map.of(
                "health", "/health",
                "api", config.getBasePath(),
                "documentation", "/swagger-ui"
        ));
        ctx.json(info);
    }

    private void handleMetrics(Context ctx) {
        long uptimeMs = System.currentTimeMillis() - startTime;

        Map<String, Object> metrics = new HashMap<>();

        // Server uptime metrics
        Map<String, Object> uptimeMetrics = new HashMap<>();
        uptimeMetrics.put("uptime_ms", uptimeMs);
        uptimeMetrics.put("uptime_seconds", uptimeMs / 1000);
        uptimeMetrics.put("uptime_minutes", uptimeMs / 60000);
        uptimeMetrics.put("uptime_hours", uptimeMs / 3600000);
        uptimeMetrics.put("start_time", startTime);
        uptimeMetrics.put("current_time", System.currentTimeMillis());
        metrics.put("uptime", uptimeMetrics);

        // Database metrics
        List<String> dbNames = new ArrayList<>();
        context.getDatabaseNames().forEach(dbNames::add);
        Map<String, Object> dbMetrics = new HashMap<>();
        dbMetrics.put("database_count", dbNames.size());
        dbMetrics.put("database_names", dbNames);
        metrics.put("databases", dbMetrics);

        // Memory metrics
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memoryMetrics = new HashMap<>();
        memoryMetrics.put("max_mb", runtime.maxMemory() / 1024 / 1024);
        memoryMetrics.put("total_mb", runtime.totalMemory() / 1024 / 1024);
        memoryMetrics.put("free_mb", runtime.freeMemory() / 1024 / 1024);
        memoryMetrics.put("used_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        memoryMetrics.put("usage_percent",
            (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100);
        metrics.put("memory", memoryMetrics);

        // Request/Response metrics from MetricsCollector
        metrics.put("requests", metricsCollector.getMetrics());

        ctx.json(metrics);
    }

    /**
     * Start the server.
     */
    public VeccyRestServer start() {
        app.start(config.getHost(), config.getPort());
        logger.info("Veccy REST API server started on {}:{}", config.getHost(), config.getPort());

        if (config.isEnableHttps()) {
            logger.info("HTTPS enabled on port {}", config.getHttpsPort());
            logger.info("API documentation available at: https://{}:{}/swagger-ui", config.getHost(), config.getHttpsPort());
        } else {
            logger.info("API documentation available at: http://{}:{}/swagger-ui", config.getHost(), config.getPort());
        }

        return this;
    }

    /**
     * Stop the server gracefully.
     * Waits for ongoing requests to complete before shutting down.
     */
    public void stop() {
        logger.info("Initiating graceful shutdown of Veccy REST API server...");

        try {
            // Step 1: Stop accepting new requests
            logger.info("Stopping Javalin server (will wait for active requests to complete)...");
            app.stop();

            // Step 2: Shutdown middleware components
            logger.info("Shutting down timeout handler...");
            timeoutHandler.shutdown();

            logger.info("Shutting down rate limiter...");
            rateLimiter.shutdown();

            // Step 3: Shutdown server context (closes databases, etc.)
            logger.info("Shutting down server context...");
            context.shutdown();

            logger.info("Graceful shutdown completed successfully.");
        } catch (Exception e) {
            logger.error("Error during graceful shutdown", e);
            throw new RuntimeException("Failed to shutdown gracefully", e);
        }
    }

    /**
     * Force immediate shutdown without waiting for ongoing requests.
     * Use only when graceful shutdown fails.
     */
    public void forceShutdown() {
        logger.warn("Forcing immediate shutdown...");
        try {
            timeoutHandler.shutdown();
        } catch (Exception e) {
            logger.error("Error shutting down timeout handler", e);
        }
        try {
            rateLimiter.shutdown();
        } catch (Exception e) {
            logger.error("Error shutting down rate limiter", e);
        }
        try {
            context.shutdown();
        } catch (Exception e) {
            logger.error("Error shutting down context", e);
        }
        try {
            app.stop();
        } catch (Exception e) {
            logger.error("Error stopping Javalin", e);
        }
        logger.info("Force shutdown completed.");
    }

    /**
     * Get the server context.
     */
    public ServerContext getContext() {
        return context;
    }

    /**
     * Get the Javalin app instance.
     */
    public Javalin getApp() {
        return app;
    }

    /**
     * Get the metrics collector.
     */
    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    /**
     * Main method for standalone server execution.
     */
    public static void main(String[] args) {
        // Parse command line arguments
        RestConfig config = parseArgs(args);

        // Create and start server
        VeccyRestServer server = new VeccyRestServer(config);
        server.start();

        // Add shutdown hook for graceful shutdown on SIGTERM/SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received. Initiating graceful shutdown...");
            try {
                server.stop();
            } catch (Exception e) {
                logger.error("Graceful shutdown failed, forcing shutdown", e);
                server.forceShutdown();
            }
        }, "shutdown-hook"));

        logger.info("Server is running. Press Ctrl+C to shutdown gracefully.");
    }

    private static RestConfig parseArgs(String[] args) {
        RestConfig.Builder builder = new RestConfig.Builder();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                case "-p":
                    if (i + 1 < args.length) {
                        builder.port(Integer.parseInt(args[++i]));
                    }
                    break;
                case "--host":
                case "-h":
                    if (i + 1 < args.length) {
                        builder.host(args[++i]);
                    }
                    break;
                case "--production":
                    return RestConfig.productionConfig();
                case "--metrics":
                    builder.enableMetrics(true);
                    break;
                case "--help":
                    printUsage();
                    System.exit(0);
                    break;
            }
        }

        return builder.build();
    }

    private static void printUsage() {
        logger.info("Veccy REST API Server");
        logger.info("");
        logger.info("Usage: java -jar veccy-rest-server.jar [options]");
        logger.info("");
        logger.info("Options:");
        logger.info("  -p, --port <port>       Server port (default: 7878)");
        logger.info("  -h, --host <host>       Server host (default: localhost)");
        logger.info("  --production            Use production configuration");
        logger.info("  --metrics               Enable metrics endpoint");
        logger.info("  --help                  Show this help message");
    }
}
