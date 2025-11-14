package com.veccy.client;

import com.veccy.base.Page;
import com.veccy.base.SearchResult;
import com.veccy.config.FlatConfig;
import com.veccy.config.Metric;
import com.veccy.exceptions.VeccyException;
import com.veccy.indices.FlatIndex;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for VectorDBClient focusing on methods not covered in VectorDBClientTest.
 */
public class VectorDBClientAdditionalTest {

    private VectorDBClient client;
    private StorageBackend storage;
    private FlatIndex index;

    @BeforeEach
    public void setUp() {
        storage = new MemoryStorage(new HashMap<>());
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();
        index = new FlatIndex(config);
        client = new VectorDBClient(storage, index);
        client.initialize();
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testBatchUpdate() {
        // Insert initial vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };
        List<String> ids = client.insert(vectors, null);

        // Prepare batch update
        List<String> updateIds = Arrays.asList(ids.get(0), ids.get(1));
        List<double[]> updateVectors = Arrays.asList(
            new double[]{0.7071, 0.7071, 0.0},
            new double[]{0.0, 0.7071, 0.7071}
        );
        List<Map<String, Object>> updateMetadata = Arrays.asList(
            Map.of("updated", true, "batch", 1),
            Map.of("updated", true, "batch", 2)
        );

        // Perform batch update
        List<Boolean> results = client.batchUpdate(updateIds, updateVectors, updateMetadata);

        assertEquals(2, results.size());
        assertTrue(results.get(0));
        assertTrue(results.get(1));

        // Verify updates
        List<SearchResult> searchResults = client.search(updateVectors.get(0), 1);
        assertEquals(ids.get(0), searchResults.get(0).getId());
        assertEquals(true, searchResults.get(0).getMetadata().get("updated"));
    }

    @Test
    public void testBatchSearch() {
        // Insert vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0},
            {0.7071, 0.7071, 0.0},
            {0.0, 0.7071, 0.7071}
        };
        client.insert(vectors, null);

        // Batch search
        double[][] queries = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0}
        };

        List<List<SearchResult>> results = client.batchSearch(queries, 3);

        assertEquals(2, results.size());
        assertEquals(3, results.get(0).size());
        assertEquals(3, results.get(1).size());

        // First query should match first vector best
        assertTrue(results.get(0).get(0).getDistance() < 0.1);
        // Second query should match second vector best
        assertTrue(results.get(1).get(0).getDistance() < 0.1);
    }

    @Test
    public void testBatchSearchBeforeInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient uninitializedClient = new VectorDBClient(newStorage, newIndex);

        assertThrows(VeccyException.class, () -> {
            uninitializedClient.batchSearch(new double[][]{{1.0, 0.0, 0.0}}, 5);
        });
    }

    @Test
    public void testListVectorIds() {
        // Insert vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };
        List<String> insertedIds = client.insert(vectors, null);

        // List all IDs
        List<String> listedIds = client.listVectorIds(null);

        assertEquals(3, listedIds.size());
        assertTrue(listedIds.containsAll(insertedIds));
    }

    @Test
    public void testListVectorIdsWithLimit() {
        // Insert vectors
        double[][] vectors = new double[10][3];
        for (int i = 0; i < 10; i++) {
            vectors[i] = new double[]{i * 0.1, i * 0.1, i * 0.1};
        }
        client.insert(vectors, null);

        // List with limit
        List<String> listedIds = client.listVectorIds(5);

        assertTrue(listedIds.size() <= 5);
    }

    @Test
    public void testListVectorIdsBeforeInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient uninitializedClient = new VectorDBClient(newStorage, newIndex);

        assertThrows(VeccyException.class, () -> {
            uninitializedClient.listVectorIds(10);
        });
    }

    @Test
    public void testListVectorIdsPaginated() {
        // Insert vectors
        double[][] vectors = new double[20][3];
        for (int i = 0; i < 20; i++) {
            vectors[i] = new double[]{i * 0.1, i * 0.1, i * 0.1};
        }
        client.insert(vectors, null);

        // First page
        Page<String> page1 = client.listVectorIdsPaginated(5, Optional.empty());

        assertNotNull(page1);
        assertTrue(page1.items().size() <= 5);

        // Second page if there is a next cursor
        if (page1.nextCursor().isPresent()) {
            Page<String> page2 = client.listVectorIdsPaginated(5, page1.nextCursor());
            assertNotNull(page2);
            assertTrue(page2.items().size() <= 5);
        }
    }

    @Test
    public void testListVectorIdsPaginatedBeforeInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient uninitializedClient = new VectorDBClient(newStorage, newIndex);

        assertThrows(VeccyException.class, () -> {
            uninitializedClient.listVectorIdsPaginated(10, Optional.empty());
        });
    }

    @Test
    public void testStreamVectorIds() {
        // Insert vectors
        double[][] vectors = new double[15][3];
        for (int i = 0; i < 15; i++) {
            vectors[i] = new double[]{i * 0.1, i * 0.1, i * 0.1};
        }
        List<String> insertedIds = client.insert(vectors, null);

        // Stream IDs
        try (Stream<String> stream = client.streamVectorIds()) {
            List<String> streamedIds = stream.collect(Collectors.toList());

            assertEquals(15, streamedIds.size());
            assertTrue(streamedIds.containsAll(insertedIds));
        }
    }

    @Test
    public void testStreamVectorIdsBeforeInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient uninitializedClient = new VectorDBClient(newStorage, newIndex);

        assertThrows(VeccyException.class, () -> {
            uninitializedClient.streamVectorIds();
        });
    }

    @Test
    public void testGetStorageBackend() {
        StorageBackend backend = client.getStorageBackend();
        assertNotNull(backend);
        assertSame(storage, backend);
    }

    @Test
    public void testGetIndex() {
        assertEquals(index, client.getIndex());
    }

    @Test
    public void testGetQuantizer() {
        assertNull(client.getQuantizer());
    }

    @Test
    public void testGetPersistenceManager() {
        assertNull(client.getPersistenceManager());
    }

    @Test
    public void testGetConfig() {
        Map<String, Object> config = client.getConfig();
        assertNotNull(config);
    }

    @Test
    public void testGetConfigWithCustomConfig() {
        Map<String, Object> customConfig = new HashMap<>();
        customConfig.put("key1", "value1");
        customConfig.put("key2", 42);

        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient configuredClient = new VectorDBClient(newStorage, newIndex, null, null, customConfig);

        Map<String, Object> retrievedConfig = configuredClient.getConfig();
        assertEquals("value1", retrievedConfig.get("key1"));
        assertEquals(42, retrievedConfig.get("key2"));

        configuredClient.close();
    }

    @Test
    public void testInsertBeforeInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient uninitializedClient = new VectorDBClient(newStorage, newIndex);

        assertThrows(VeccyException.class, () -> {
            uninitializedClient.insert(new double[][]{{1.0, 0.0, 0.0}}, null);
        });
    }

    @Test
    public void testSearchBeforeInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient uninitializedClient = new VectorDBClient(newStorage, newIndex);

        assertThrows(VeccyException.class, () -> {
            uninitializedClient.search(new double[]{1.0, 0.0, 0.0}, 5);
        });
    }

    @Test
    public void testDeleteBeforeInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient uninitializedClient = new VectorDBClient(newStorage, newIndex);

        assertThrows(VeccyException.class, () -> {
            uninitializedClient.delete(List.of("id1"));
        });
    }

    @Test
    public void testUpdateBeforeInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient uninitializedClient = new VectorDBClient(newStorage, newIndex);

        assertThrows(VeccyException.class, () -> {
            uninitializedClient.update("id1", new double[]{1.0, 0.0, 0.0}, null);
        });
    }

    @Test
    public void testGetStatsBeforeInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient uninitializedClient = new VectorDBClient(newStorage, newIndex);

        assertThrows(VeccyException.class, () -> {
            uninitializedClient.getStats();
        });
    }

    @Test
    public void testCloseIdempotent() {
        client.insert(new double[][]{{1.0, 0.0, 0.0}}, null);

        client.close();
        assertFalse(client.isInitialized());

        // Second close should be safe
        client.close();
        assertFalse(client.isInitialized());

        // Third close should be safe
        client.close();
        assertFalse(client.isInitialized());
    }

    @Test
    public void testConstructorWithNullConfig() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatIndex newIndex = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient clientWithNullConfig = new VectorDBClient(newStorage, newIndex, null, null, null);

        assertNotNull(clientWithNullConfig.getConfig());
        assertEquals(0, clientWithNullConfig.getConfig().size());

        clientWithNullConfig.close();
    }

    @Test
    public void testBatchUpdateWithNullVectors() {
        // Insert initial vectors
        double[][] vectors = {{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}};
        List<String> ids = client.insert(vectors, null);

        // Update metadata only (null vectors)
        List<String> updateIds = List.of(ids.get(0));
        List<double[]> nullVectors = Arrays.asList((double[]) null);
        List<Map<String, Object>> updateMetadata = List.of(Map.of("updated", "metadata_only"));

        List<Boolean> results = client.batchUpdate(updateIds, nullVectors, updateMetadata);

        assertEquals(1, results.size());
        assertTrue(results.get(0));
    }

    @Test
    public void testBatchSearchWithMultipleQueries() {
        // Insert many vectors
        Random random = new Random(42);
        double[][] vectors = new double[50][32];
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 32; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }
        client.insert(vectors, null);

        // Batch search with multiple queries
        double[][] queries = new double[10][32];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 32; j++) {
                queries[i][j] = random.nextGaussian();
            }
        }

        List<List<SearchResult>> results = client.batchSearch(queries, 5);

        assertEquals(10, results.size());
        for (List<SearchResult> queryResults : results) {
            assertEquals(5, queryResults.size());
        }
    }

    @Test
    public void testStreamVectorIdsEmpty() {
        // No vectors inserted
        try (Stream<String> stream = client.streamVectorIds()) {
            List<String> ids = stream.collect(Collectors.toList());
            assertEquals(0, ids.size());
        }
    }

    @Test
    public void testListVectorIdsPaginatedEmpty() {
        // No vectors inserted
        Page<String> page = client.listVectorIdsPaginated(10, Optional.empty());

        assertNotNull(page);
        assertEquals(0, page.items().size());
        assertFalse(page.nextCursor().isPresent());
    }
}
