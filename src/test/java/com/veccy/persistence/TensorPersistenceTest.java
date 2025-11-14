package com.veccy.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TensorPersistence.
 */
public class TensorPersistenceTest {

    @TempDir
    Path tempDir;

    private TensorPersistence persistence;
    private Map<String, Object> config;

    @BeforeEach
    public void setUp() {
        config = new HashMap<>();
        config.put("data_dir", tempDir.toString());
        config.put("compression", false);
        persistence = new TensorPersistence(config);
        persistence.initialize();
    }

    @AfterEach
    public void tearDown() {
        if (persistence != null) {
            persistence.close();
        }
    }

    @Test
    public void testInitialization() {
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("data_dir", tempDir.resolve("new_persist").toString());

        TensorPersistence newPersistence = new TensorPersistence(newConfig);
        assertFalse(newPersistence.isInitialized());

        newPersistence.initialize();
        assertTrue(newPersistence.isInitialized());

        // Verify directory was created
        assertTrue(Files.exists(tempDir.resolve("new_persist")));

        newPersistence.close();
    }

    @Test
    public void testSaveState_Simple() {
        Map<String, Object> state = new HashMap<>();
        state.put("version", 1);
        state.put("timestamp", System.currentTimeMillis());
        state.put("vector_count", 100);

        boolean result = persistence.saveState(state, "state.json");
        assertTrue(result);

        // Verify file exists
        assertTrue(Files.exists(tempDir.resolve("state.json")));
    }

    @Test
    public void testLoadState_Simple() {
        Map<String, Object> state = new HashMap<>();
        state.put("version", 1);
        state.put("timestamp", 123456789L);
        state.put("vector_count", 100);

        persistence.saveState(state, "state.json");

        Optional<Map<String, Object>> loaded = persistence.loadState("state.json");
        assertTrue(loaded.isPresent());

        Map<String, Object> loadedState = loaded.get();
        assertEquals(1, loadedState.get("version"));
        assertEquals(123456789L, ((Number) loadedState.get("timestamp")).longValue());
        assertEquals(100, loadedState.get("vector_count"));
    }

    @Test
    public void testLoadState_NotExists() {
        Optional<Map<String, Object>> loaded = persistence.loadState("nonexistent.json");
        assertFalse(loaded.isPresent());
    }

    @Test
    public void testSaveVectors_Simple() {
        double[][] vectors = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0},
            {7.0, 8.0, 9.0}
        };

        List<String> ids = Arrays.asList("vec1", "vec2", "vec3");

        boolean result = persistence.saveVectors(vectors, ids, "vectors.bin");
        assertTrue(result);

        // Verify file exists
        assertTrue(Files.exists(tempDir.resolve("vectors.bin")));
    }

    @Test
    public void testLoadVectors_Simple() {
        double[][] vectors = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0},
            {7.0, 8.0, 9.0}
        };

        List<String> ids = Arrays.asList("vec1", "vec2", "vec3");

        persistence.saveVectors(vectors, ids, "vectors.bin");

        Optional<PersistenceManager.VectorsWithIds> loaded = persistence.loadVectors("vectors.bin");
        assertTrue(loaded.isPresent());

        PersistenceManager.VectorsWithIds vwi = loaded.get();
        assertEquals(3, vwi.getVectors().length);
        assertEquals(3, vwi.getIds().size());

        // Verify vectors
        for (int i = 0; i < vectors.length; i++) {
            assertArrayEquals(vectors[i], vwi.getVectors()[i], 1e-6);
        }

        // Verify IDs
        assertEquals(ids, vwi.getIds());
    }

    @Test
    public void testLoadVectors_NotExists() {
        Optional<PersistenceManager.VectorsWithIds> loaded = persistence.loadVectors("nonexistent.bin");
        assertFalse(loaded.isPresent());
    }

    @Test
    public void testSaveIndex_Simple() {
        Map<String, Object> indexData = new HashMap<>();
        indexData.put("type", "hnsw");
        indexData.put("m", 16);
        indexData.put("ef_construction", 200);
        indexData.put("max_level", 3);

        boolean result = persistence.saveIndex(indexData, "index.json");
        assertTrue(result);

        // Verify file exists
        assertTrue(Files.exists(tempDir.resolve("index.json")));
    }

    @Test
    public void testLoadIndex_Simple() {
        Map<String, Object> indexData = new HashMap<>();
        indexData.put("type", "hnsw");
        indexData.put("m", 16);
        indexData.put("ef_construction", 200);
        indexData.put("max_level", 3);

        persistence.saveIndex(indexData, "index.json");

        Optional<Map<String, Object>> loaded = persistence.loadIndex("index.json");
        assertTrue(loaded.isPresent());

        Map<String, Object> loadedIndex = loaded.get();
        assertEquals("hnsw", loadedIndex.get("type"));
        assertEquals(16, loadedIndex.get("m"));
        assertEquals(200, loadedIndex.get("ef_construction"));
        assertEquals(3, loadedIndex.get("max_level"));
    }

    @Test
    public void testLoadIndex_NotExists() {
        Optional<Map<String, Object>> loaded = persistence.loadIndex("nonexistent.json");
        assertFalse(loaded.isPresent());
    }

    @Test
    public void testCompression() {
        // Create persistence with compression enabled
        Map<String, Object> compressedConfig = new HashMap<>();
        compressedConfig.put("data_dir", tempDir.resolve("compressed").toString());
        compressedConfig.put("compression", true);

        TensorPersistence compressedPersistence = new TensorPersistence(compressedConfig);
        compressedPersistence.initialize();

        try {
            // Save state with compression
            Map<String, Object> state = new HashMap<>();
            state.put("large_data", "x".repeat(10000));

            boolean saved = compressedPersistence.saveState(state, "state.json.gz");
            assertTrue(saved);

            // Load and verify
            Optional<Map<String, Object>> loaded = compressedPersistence.loadState("state.json.gz");
            assertTrue(loaded.isPresent());
            assertEquals("x".repeat(10000), loaded.get().get("large_data"));

            // Compressed file should exist
            assertTrue(Files.exists(tempDir.resolve("compressed/state.json.gz")));
        } finally {
            compressedPersistence.close();
        }
    }

    @Test
    public void testGetStats() {
        // Save some files
        persistence.saveState(Map.of("test", "data"), "state.json");
        persistence.saveVectors(new double[][]{{1.0, 2.0}}, List.of("vec1"), "vectors.bin");

        Map<String, Object> stats = persistence.getStats();

        assertNotNull(stats);
        assertEquals("TensorPersistence", stats.get("type"));
        assertEquals(false, stats.get("compression_enabled"));
        assertTrue(stats.containsKey("data_directory"));
    }

    @Test
    public void testComplexState() {
        // Test with nested structures
        Map<String, Object> state = new HashMap<>();
        state.put("version", 2);
        state.put("config", Map.of("metric", "cosine", "m", 16));
        state.put("stats", Map.of("count", 100, "dimensions", 128));
        state.put("tags", Arrays.asList("production", "v2", "optimized"));

        persistence.saveState(state, "complex_state.json");

        Optional<Map<String, Object>> loaded = persistence.loadState("complex_state.json");
        assertTrue(loaded.isPresent());

        Map<String, Object> loadedState = loaded.get();
        assertEquals(2, loadedState.get("version"));

        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) loadedState.get("config");
        assertEquals("cosine", config.get("metric"));
        assertEquals(16, config.get("m"));
    }

    @Test
    public void testLargeVectors() {
        // Test with high-dimensional vectors
        int count = 1000;
        int dimensions = 512;

        double[][] vectors = new double[count][dimensions];
        Random random = new Random(42);

        for (int i = 0; i < count; i++) {
            for (int j = 0; j < dimensions; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add("vec" + i);
        }

        // Save
        boolean saved = persistence.saveVectors(vectors, ids, "large_vectors.bin");
        assertTrue(saved);

        // Load and verify
        Optional<PersistenceManager.VectorsWithIds> loaded = persistence.loadVectors("large_vectors.bin");
        assertTrue(loaded.isPresent());

        PersistenceManager.VectorsWithIds vwi = loaded.get();
        assertEquals(count, vwi.getVectors().length);
        assertEquals(count, vwi.getIds().size());

        // Verify sample of vectors
        for (int i = 0; i < 10; i++) {
            int idx = random.nextInt(count);
            assertArrayEquals(vectors[idx], vwi.getVectors()[idx], 1e-6);
            assertEquals("vec" + idx, vwi.getIds().get(idx));
        }
    }

    @Test
    public void testMultipleSaveLoad() {
        // Test saving and loading multiple times
        for (int i = 0; i < 5; i++) {
            Map<String, Object> state = Map.of("iteration", i, "timestamp", System.currentTimeMillis());
            persistence.saveState(state, "state" + i + ".json");
        }

        // Load all
        for (int i = 0; i < 5; i++) {
            Optional<Map<String, Object>> loaded = persistence.loadState("state" + i + ".json");
            assertTrue(loaded.isPresent());
            assertEquals(i, loaded.get().get("iteration"));
        }
    }

    @Test
    public void testOverwrite() {
        // Save initial state
        Map<String, Object> state1 = Map.of("version", 1);
        persistence.saveState(state1, "state.json");

        // Overwrite with new state
        Map<String, Object> state2 = Map.of("version", 2);
        persistence.saveState(state2, "state.json");

        // Load should get the new state
        Optional<Map<String, Object>> loaded = persistence.loadState("state.json");
        assertTrue(loaded.isPresent());
        assertEquals(2, loaded.get().get("version"));
    }

    @Test
    public void testVectorPrecision() {
        // Test that floating point precision is preserved
        double[][] vectors = {
            {Math.PI, Math.E, Math.sqrt(2)},
            {1.23456789012345, 9.87654321098765, 5.555555555555}
        };

        List<String> ids = Arrays.asList("vec1", "vec2");

        persistence.saveVectors(vectors, ids, "precise_vectors.bin");

        Optional<PersistenceManager.VectorsWithIds> loaded = persistence.loadVectors("precise_vectors.bin");
        assertTrue(loaded.isPresent());

        double[][] loadedVectors = loaded.get().getVectors();

        // Check high precision preservation
        assertEquals(Math.PI, loadedVectors[0][0], 1e-10);
        assertEquals(Math.E, loadedVectors[0][1], 1e-10);
        assertEquals(Math.sqrt(2), loadedVectors[0][2], 1e-10);
    }

    @Test
    public void testEmptyVectors() {
        double[][] vectors = {};
        List<String> ids = new ArrayList<>();

        // Should handle empty arrays
        boolean result = persistence.saveVectors(vectors, ids, "empty_vectors.bin");
        assertTrue(result);

        Optional<PersistenceManager.VectorsWithIds> loaded = persistence.loadVectors("empty_vectors.bin");
        assertTrue(loaded.isPresent());
        assertEquals(0, loaded.get().getVectors().length);
        assertEquals(0, loaded.get().getIds().size());
    }

    @Test
    public void testSpecialCharactersInIds() {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}, {5.0, 6.0}};
        List<String> ids = Arrays.asList("vec:with:colons", "vec/with/slashes", "vec with spaces");

        persistence.saveVectors(vectors, ids, "special_ids.bin");

        Optional<PersistenceManager.VectorsWithIds> loaded = persistence.loadVectors("special_ids.bin");
        assertTrue(loaded.isPresent());
        assertEquals(ids, loaded.get().getIds());
    }

    @Test
    public void testNestedIndexData() {
        // Test with complex nested index structure
        Map<String, Object> indexData = new HashMap<>();
        indexData.put("type", "hnsw");

        Map<String, Object> graph = new HashMap<>();
        graph.put("level0", Arrays.asList(1, 2, 3, 4));
        graph.put("level1", Arrays.asList(5, 6));
        indexData.put("graph", graph);

        Map<String, Object> params = new HashMap<>();
        params.put("m", 16);
        params.put("ef_construction", 200);
        indexData.put("params", params);

        persistence.saveIndex(indexData, "complex_index.json");

        Optional<Map<String, Object>> loaded = persistence.loadIndex("complex_index.json");
        assertTrue(loaded.isPresent());

        @SuppressWarnings("unchecked")
        Map<String, Object> loadedGraph = (Map<String, Object>) loaded.get().get("graph");
        assertNotNull(loadedGraph);

        @SuppressWarnings("unchecked")
        List<Integer> level0 = (List<Integer>) loadedGraph.get("level0");
        assertEquals(Arrays.asList(1, 2, 3, 4), level0);
    }

    @Test
    public void testPersistenceAcrossInstances() {
        // Save with one instance
        Map<String, Object> state = Map.of("value", 42);
        persistence.saveState(state, "state.json");

        double[][] vectors = {{1.0, 2.0, 3.0}};
        List<String> ids = List.of("vec1");
        persistence.saveVectors(vectors, ids, "vectors.bin");

        // Close
        persistence.close();

        // Create new instance
        TensorPersistence newPersistence = new TensorPersistence(config);
        newPersistence.initialize();

        try {
            // Load with new instance
            Optional<Map<String, Object>> loadedState = newPersistence.loadState("state.json");
            assertTrue(loadedState.isPresent());
            assertEquals(42, loadedState.get().get("value"));

            Optional<PersistenceManager.VectorsWithIds> loadedVectors = newPersistence.loadVectors("vectors.bin");
            assertTrue(loadedVectors.isPresent());
            assertArrayEquals(vectors[0], loadedVectors.get().getVectors()[0], 1e-6);
        } finally {
            newPersistence.close();
        }
    }

    @Test
    public void testClose() {
        persistence.saveState(Map.of("test", "data"), "state.json");
        assertTrue(persistence.isInitialized());

        persistence.close();

        // Files should still exist
        assertTrue(Files.exists(tempDir.resolve("state.json")));

        // Stats should still be accessible
        Map<String, Object> stats = persistence.getStats();
        assertNotNull(stats);
    }
}
