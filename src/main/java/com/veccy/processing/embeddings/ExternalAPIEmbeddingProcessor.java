package com.veccy.processing.embeddings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veccy.exceptions.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * External API-based embedding processor.
 * <p>
 * Supports various embedding API providers:
 * - OpenAI (text-embedding-ada-002, text-embedding-3-small, etc.)
 * - Cohere (embed-english-v3.0, embed-multilingual-v3.0, etc.)
 * - Custom APIs following similar patterns
 * <p>
 * Configuration:
 * - provider: "openai" or "cohere" or "custom"
 * - api_key: API authentication key
 * - model: Model name (e.g., "text-embedding-ada-002")
 * - api_url: Custom API endpoint (for custom provider)
 * - dimensions: Output dimensions (optional, auto-detected)
 * - timeout_seconds: Request timeout (default 30)
 */
public class ExternalAPIEmbeddingProcessor implements EmbeddingProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ExternalAPIEmbeddingProcessor.class);

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/embeddings";
    private static final String COHERE_API_URL = "https://api.cohere.ai/v1/embed";

    private Map<String, Object> config;
    private String provider;
    private String apiKey;
    private String model;
    private String apiUrl;
    private int dimensions;
    private int timeoutSeconds;
    private boolean initialized;

    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    public ExternalAPIEmbeddingProcessor() {
        this.initialized = false;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void initialize(Map<String, Object> config) {
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();

        // Get provider
        this.provider = (String) this.config.getOrDefault("provider", "openai");

        // Get API key
        this.apiKey = (String) this.config.get("api_key");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ProcessingException("api_key is required in config");
        }

        // Get model
        this.model = (String) this.config.getOrDefault("model", getDefaultModel(provider));

        // Get API URL
        if (this.config.containsKey("api_url")) {
            this.apiUrl = (String) this.config.get("api_url");
        } else {
            this.apiUrl = getDefaultApiUrl(provider);
        }

        // Get dimensions (optional)
        if (this.config.containsKey("dimensions")) {
            this.dimensions = ((Number) this.config.get("dimensions")).intValue();
        } else {
            this.dimensions = getDefaultDimensions(provider, model);
        }

        // Get timeout
        this.timeoutSeconds = ((Number) this.config.getOrDefault("timeout_seconds", 30)).intValue();

        // Create HTTP client
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        initialized = true;
        logger.info("External API processor initialized: provider={}, model={}, dims={}",
                provider, model, dimensions);
    }

    @Override
    public double[] embed(String text) {
        if (!initialized) {
            throw new ProcessingException("Processor not initialized");
        }

        try {
            // Create request payload
            String requestBody = createRequestPayload(Collections.singletonList(text));

            // Send HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ProcessingException("API request failed with status " + response.statusCode() +
                        ": " + response.body());
            }

            // Parse response
            double[][] embeddings = parseResponse(response.body());

            return embeddings[0];

        } catch (IOException | InterruptedException e) {
            throw new ProcessingException("Failed to call embedding API: " + e.getMessage(), e);
        }
    }

    @Override
    public double[][] embedBatch(List<String> texts) {
        if (!initialized) {
            throw new ProcessingException("Processor not initialized");
        }

        try {
            // Create request payload
            String requestBody = createRequestPayload(texts);

            // Send HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ProcessingException("API request failed with status " + response.statusCode() +
                        ": " + response.body());
            }

            // Parse response
            double[][] embeddings = parseResponse(response.body());

            logger.debug("Generated {} embeddings from {} API", embeddings.length, provider);
            return embeddings;

        } catch (IOException | InterruptedException e) {
            throw new ProcessingException("Failed to call embedding API: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String getName() {
        return "External API Embedding Processor (" + provider + ")";
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("type", "ExternalAPIEmbeddingProcessor");
        stats.put("name", getName());
        stats.put("initialized", initialized);
        stats.put("provider", provider);
        stats.put("model", model);
        stats.put("api_url", apiUrl);
        stats.put("dimensions", dimensions);
        stats.put("timeout_seconds", timeoutSeconds);
        return stats;
    }

    @Override
    public void close() {
        // HTTP client doesn't need explicit closing in Java 11+
        initialized = false;
        logger.info("External API processor closed");
    }

    /**
     * Creates JSON request payload for the API.
     */
    private String createRequestPayload(List<String> texts) throws IOException {
        Map<String, Object> payload = new HashMap<>();

        switch (provider.toLowerCase()) {
            case "openai":
                payload.put("input", texts);
                payload.put("model", model);
                break;

            case "cohere":
                payload.put("texts", texts);
                payload.put("model", model);
                payload.put("input_type", "search_document");
                break;

            case "custom":
                // Assume OpenAI-like format for custom
                payload.put("input", texts);
                payload.put("model", model);
                break;

            default:
                throw new ProcessingException("Unsupported provider: " + provider);
        }

        return objectMapper.writeValueAsString(payload);
    }

    /**
     * Parses JSON response from the API.
     */
    private double[][] parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        switch (provider.toLowerCase()) {
            case "openai":
                return parseOpenAIResponse(root);

            case "cohere":
                return parseCohereResponse(root);

            case "custom":
                return parseOpenAIResponse(root);  // Assume OpenAI format

            default:
                throw new ProcessingException("Unsupported provider: " + provider);
        }
    }

    /**
     * Parses OpenAI API response format.
     */
    private double[][] parseOpenAIResponse(JsonNode root) {
        JsonNode data = root.get("data");
        if (data == null || !data.isArray()) {
            throw new ProcessingException("Invalid OpenAI response format");
        }

        double[][] embeddings = new double[data.size()][];

        for (int i = 0; i < data.size(); i++) {
            JsonNode embeddingNode = data.get(i).get("embedding");
            if (embeddingNode == null || !embeddingNode.isArray()) {
                throw new ProcessingException("Invalid embedding format in response");
            }

            embeddings[i] = new double[embeddingNode.size()];
            for (int j = 0; j < embeddingNode.size(); j++) {
                embeddings[i][j] = embeddingNode.get(j).asDouble();
            }
        }

        return embeddings;
    }

    /**
     * Parses Cohere API response format.
     */
    private double[][] parseCohereResponse(JsonNode root) {
        JsonNode embeddings = root.get("embeddings");
        if (embeddings == null || !embeddings.isArray()) {
            throw new ProcessingException("Invalid Cohere response format");
        }

        double[][] result = new double[embeddings.size()][];

        for (int i = 0; i < embeddings.size(); i++) {
            JsonNode embeddingNode = embeddings.get(i);
            if (embeddingNode == null || !embeddingNode.isArray()) {
                throw new ProcessingException("Invalid embedding format in response");
            }

            result[i] = new double[embeddingNode.size()];
            for (int j = 0; j < embeddingNode.size(); j++) {
                result[i][j] = embeddingNode.get(j).asDouble();
            }
        }

        return result;
    }

    /**
     * Gets default API URL for a provider.
     */
    private String getDefaultApiUrl(String provider) {
        switch (provider.toLowerCase()) {
            case "openai":
                return OPENAI_API_URL;
            case "cohere":
                return COHERE_API_URL;
            default:
                throw new ProcessingException("Unknown provider: " + provider +
                        ". Please specify api_url in config.");
        }
    }

    /**
     * Gets default model for a provider.
     */
    private String getDefaultModel(String provider) {
        switch (provider.toLowerCase()) {
            case "openai":
                return "text-embedding-ada-002";
            case "cohere":
                return "embed-english-v3.0";
            default:
                return "unknown";
        }
    }

    /**
     * Gets default dimensions for a provider/model.
     */
    private int getDefaultDimensions(String provider, String model) {
        switch (provider.toLowerCase()) {
            case "openai":
                if (model.contains("ada-002")) {
                    return 1536;
                } else if (model.contains("3-small")) {
                    return 1536;
                } else if (model.contains("3-large")) {
                    return 3072;
                }
                return 1536;  // Default

            case "cohere":
                return 1024;  // Cohere v3 default

            default:
                return 768;  // Generic default
        }
    }
}
