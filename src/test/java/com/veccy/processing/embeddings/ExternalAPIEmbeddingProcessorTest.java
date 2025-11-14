package com.veccy.processing.embeddings;

import com.veccy.exceptions.ProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ExternalAPIEmbeddingProcessor.
 * Note: These tests focus on configuration validation and error handling.
 * Actual API calls are not tested to avoid external dependencies.
 */
class ExternalAPIEmbeddingProcessorTest {

    private ExternalAPIEmbeddingProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ExternalAPIEmbeddingProcessor();
    }

    @Test
    void testConstructor() {
        assertNotNull(processor);
        assertFalse(processor.isInitialized());
    }

    @Test
    void testInitializeWithoutApiKey() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "openai");
        config.put("model", "text-embedding-ada-002");

        assertThrows(ProcessingException.class, () -> {
            processor.initialize(config);
        });
    }

    @Test
    void testInitializeWithEmptyApiKey() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "openai");
        config.put("api_key", "");

        assertThrows(ProcessingException.class, () -> {
            processor.initialize(config);
        });
    }

    @Test
    void testInitializeWithApiKey() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "openai");
        config.put("api_key", "test-api-key");

        processor.initialize(config);

        assertTrue(processor.isInitialized());
    }

    @Test
    void testInitializeWithNullConfig() {
        // Should throw because api_key is required
        assertThrows(ProcessingException.class, () -> {
            processor.initialize(null);
        });
    }

    @Test
    void testInitializeWithDefaultProvider() {
        Map<String, Object> config = new HashMap<>();
        config.put("api_key", "test-key");

        processor.initialize(config);

        assertTrue(processor.isInitialized());
        assertEquals("External API Embedding Processor (openai)", processor.getName());
    }

    @Test
    void testInitializeWithCohereProvider() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "cohere");
        config.put("api_key", "test-key");

        processor.initialize(config);

        assertTrue(processor.isInitialized());
        assertEquals("External API Embedding Processor (cohere)", processor.getName());
    }

    @Test
    void testInitializeWithCustomProvider() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "custom");
        config.put("api_key", "test-key");
        config.put("api_url", "https://custom.api.com/embeddings");

        processor.initialize(config);

        assertTrue(processor.isInitialized());
        assertEquals("External API Embedding Processor (custom)", processor.getName());
    }

    @Test
    void testInitializeWithCustomDimensions() {
        Map<String, Object> config = new HashMap<>();
        config.put("api_key", "test-key");
        config.put("dimensions", 768);

        processor.initialize(config);

        assertEquals(768, processor.getDimensions());
    }

    @Test
    void testInitializeWithCustomModel() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "openai");
        config.put("api_key", "test-key");
        config.put("model", "text-embedding-3-large");

        processor.initialize(config);

        assertTrue(processor.isInitialized());
    }

    @Test
    void testInitializeWithCustomTimeout() {
        Map<String, Object> config = new HashMap<>();
        config.put("api_key", "test-key");
        config.put("timeout_seconds", 60);

        processor.initialize(config);

        assertTrue(processor.isInitialized());
    }

    @Test
    void testGetDimensionsOpenAIAda() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "openai");
        config.put("api_key", "test-key");
        config.put("model", "text-embedding-ada-002");

        processor.initialize(config);

        assertEquals(1536, processor.getDimensions());
    }

    @Test
    void testGetDimensionsOpenAI3Small() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "openai");
        config.put("api_key", "test-key");
        config.put("model", "text-embedding-3-small");

        processor.initialize(config);

        assertEquals(1536, processor.getDimensions());
    }

    @Test
    void testGetDimensionsOpenAI3Large() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "openai");
        config.put("api_key", "test-key");
        config.put("model", "text-embedding-3-large");

        processor.initialize(config);

        assertEquals(3072, processor.getDimensions());
    }

    @Test
    void testGetDimensionsCohere() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "cohere");
        config.put("api_key", "test-key");

        processor.initialize(config);

        assertEquals(1024, processor.getDimensions());
    }

    @Test
    void testGetDimensionsCustomProvider() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "custom");
        config.put("api_key", "test-key");
        config.put("api_url", "https://custom.api.com/embeddings");

        processor.initialize(config);

        assertEquals(768, processor.getDimensions()); // Generic default
    }

    @Test
    void testEmbedNotInitialized() {
        assertThrows(ProcessingException.class, () -> {
            processor.embed("test text");
        });
    }

    @Test
    void testEmbedBatchNotInitialized() {
        assertThrows(ProcessingException.class, () -> {
            processor.embedBatch(Arrays.asList("text1", "text2"));
        });
    }

    @Test
    void testGetName() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "openai");
        config.put("api_key", "test-key");

        processor.initialize(config);

        assertEquals("External API Embedding Processor (openai)", processor.getName());
    }

    @Test
    void testIsInitializedFalse() {
        assertFalse(processor.isInitialized());
    }

    @Test
    void testIsInitializedTrue() {
        Map<String, Object> config = new HashMap<>();
        config.put("api_key", "test-key");

        processor.initialize(config);

        assertTrue(processor.isInitialized());
    }

    @Test
    void testGetStatsBeforeInitialization() {
        Map<String, Object> stats = processor.getStats();

        assertEquals("ExternalAPIEmbeddingProcessor", stats.get("type"));
        assertEquals(false, stats.get("initialized"));
    }

    @Test
    void testGetStatsAfterInitialization() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "openai");
        config.put("api_key", "test-key");
        config.put("model", "text-embedding-ada-002");

        processor.initialize(config);

        Map<String, Object> stats = processor.getStats();

        assertEquals("ExternalAPIEmbeddingProcessor", stats.get("type"));
        assertEquals(true, stats.get("initialized"));
        assertEquals("openai", stats.get("provider"));
        assertEquals("text-embedding-ada-002", stats.get("model"));
        assertEquals(1536, stats.get("dimensions"));
        assertNotNull(stats.get("api_url"));
        assertNotNull(stats.get("timeout_seconds"));
    }

    @Test
    void testClose() {
        Map<String, Object> config = new HashMap<>();
        config.put("api_key", "test-key");

        processor.initialize(config);
        assertTrue(processor.isInitialized());

        processor.close();
        assertFalse(processor.isInitialized());
    }

    @Test
    void testCloseNotInitialized() {
        processor.close();
        assertFalse(processor.isInitialized());
    }

    @Test
    void testConfigWithIntegerValues() {
        Map<String, Object> config = new HashMap<>();
        config.put("api_key", "test-key");
        config.put("dimensions", Integer.valueOf(512));
        config.put("timeout_seconds", Integer.valueOf(45));

        processor.initialize(config);

        assertEquals(512, processor.getDimensions());
    }

    @Test
    void testConfigWithDoubleValues() {
        Map<String, Object> config = new HashMap<>();
        config.put("api_key", "test-key");
        config.put("dimensions", Double.valueOf(512.5));
        config.put("timeout_seconds", Double.valueOf(45.7));

        processor.initialize(config);

        assertEquals(512, processor.getDimensions());
    }

    @Test
    void testMultipleProviders() {
        String[] providers = {"openai", "cohere", "custom"};

        for (String provider : providers) {
            ExternalAPIEmbeddingProcessor p = new ExternalAPIEmbeddingProcessor();
            Map<String, Object> config = new HashMap<>();
            config.put("provider", provider);
            config.put("api_key", "test-key");

            if (provider.equals("custom")) {
                config.put("api_url", "https://custom.api.com/embeddings");
            }

            p.initialize(config);

            assertTrue(p.isInitialized());
            assertTrue(p.getName().contains(provider));
        }
    }

    @Test
    void testDefaultModels() {
        Map<String, String> providerDefaultModels = new HashMap<>();
        providerDefaultModels.put("openai", "text-embedding-ada-002");
        providerDefaultModels.put("cohere", "embed-english-v3.0");

        for (Map.Entry<String, String> entry : providerDefaultModels.entrySet()) {
            ExternalAPIEmbeddingProcessor p = new ExternalAPIEmbeddingProcessor();
            Map<String, Object> config = new HashMap<>();
            config.put("provider", entry.getKey());
            config.put("api_key", "test-key");

            p.initialize(config);

            Map<String, Object> stats = p.getStats();
            assertEquals(entry.getValue(), stats.get("model"));
        }
    }

    @Test
    void testCustomApiUrl() {
        String customUrl = "https://my-custom-api.com/v1/embeddings";

        Map<String, Object> config = new HashMap<>();
        config.put("provider", "custom");
        config.put("api_key", "test-key");
        config.put("api_url", customUrl);

        processor.initialize(config);

        Map<String, Object> stats = processor.getStats();
        assertEquals(customUrl, stats.get("api_url"));
    }

    @Test
    void testDefaultTimeoutSeconds() {
        Map<String, Object> config = new HashMap<>();
        config.put("api_key", "test-key");

        processor.initialize(config);

        Map<String, Object> stats = processor.getStats();
        assertEquals(30, stats.get("timeout_seconds"));
    }

    @Test
    void testExplicitDimensionsOverrideDefaults() {
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "openai");
        config.put("api_key", "test-key");
        config.put("model", "text-embedding-ada-002");
        config.put("dimensions", 512); // Override default 1536

        processor.initialize(config);

        assertEquals(512, processor.getDimensions());
    }

    @Test
    void testCaseInsensitiveProvider() {
        String[] providerVariants = {"OpenAI", "OPENAI", "openai", "OpEnAi"};

        for (String variant : providerVariants) {
            ExternalAPIEmbeddingProcessor p = new ExternalAPIEmbeddingProcessor();
            Map<String, Object> config = new HashMap<>();
            config.put("provider", variant);
            config.put("api_key", "test-key");

            p.initialize(config);

            assertTrue(p.isInitialized());
        }
    }
}
