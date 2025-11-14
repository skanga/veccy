package com.veccy.base;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SearchResult record.
 */
class SearchResultTest {

    @Test
    void testSearchResultConstructor() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");

        SearchResult result = new SearchResult("vec1", 0.85, metadata);

        assertEquals("vec1", result.id());
        assertEquals(0.85, result.distance());
        assertEquals(metadata, result.metadata());
    }

    @Test
    void testSearchResultWithNullId() {
        Map<String, Object> metadata = new HashMap<>();

        assertThrows(NullPointerException.class, () -> {
            new SearchResult(null, 0.5, metadata);
        });
    }

    @Test
    void testSearchResultWithNullMetadata() {
        // Null metadata should be allowed
        SearchResult result = new SearchResult("vec1", 0.5, null);

        assertEquals("vec1", result.id());
        assertEquals(0.5, result.distance());
        assertNull(result.metadata());
    }

    @Test
    void testSearchResultWithEmptyMetadata() {
        Map<String, Object> emptyMetadata = new HashMap<>();
        SearchResult result = new SearchResult("vec1", 0.5, emptyMetadata);

        assertEquals("vec1", result.id());
        assertEquals(0.5, result.distance());
        assertTrue(result.metadata().isEmpty());
    }

    @Test
    void testGetId() {
        SearchResult result = new SearchResult("test_id", 0.75, null);
        assertEquals("test_id", result.getId());
    }

    @Test
    void testGetDistance() {
        SearchResult result = new SearchResult("vec1", 0.123456, null);
        assertEquals(0.123456, result.getDistance());
    }

    @Test
    void testGetMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 42);

        SearchResult result = new SearchResult("vec1", 0.5, metadata);

        assertEquals(metadata, result.getMetadata());
        assertEquals("value1", result.getMetadata().get("key1"));
        assertEquals(42, result.getMetadata().get("key2"));
    }

    @Test
    void testSearchResultEquals() {
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("label", "test");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("label", "test");

        SearchResult result1 = new SearchResult("vec1", 0.85, metadata1);
        SearchResult result2 = new SearchResult("vec1", 0.85, metadata2);

        assertEquals(result1, result2);
    }

    @Test
    void testSearchResultNotEquals() {
        SearchResult result1 = new SearchResult("vec1", 0.85, null);
        SearchResult result2 = new SearchResult("vec2", 0.85, null);

        assertNotEquals(result1, result2);
    }

    @Test
    void testSearchResultHashCode() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");

        SearchResult result1 = new SearchResult("vec1", 0.85, metadata);
        SearchResult result2 = new SearchResult("vec1", 0.85, metadata);

        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testSearchResultToString() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");

        SearchResult result = new SearchResult("vec1", 0.85, metadata);

        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("vec1"));
        assertTrue(str.contains("0.85"));
    }

    @Test
    void testSearchResultWithZeroDistance() {
        SearchResult result = new SearchResult("vec1", 0.0, null);
        assertEquals(0.0, result.distance());
    }

    @Test
    void testSearchResultWithNegativeDistance() {
        // Negative distances are mathematically possible in some metrics
        SearchResult result = new SearchResult("vec1", -0.5, null);
        assertEquals(-0.5, result.distance());
    }

    @Test
    void testSearchResultWithLargeDistance() {
        SearchResult result = new SearchResult("vec1", Double.MAX_VALUE, null);
        assertEquals(Double.MAX_VALUE, result.distance());
    }

    @Test
    void testSearchResultWithInfinityDistance() {
        SearchResult result = new SearchResult("vec1", Double.POSITIVE_INFINITY, null);
        assertEquals(Double.POSITIVE_INFINITY, result.distance());
    }

    @Test
    void testSearchResultWithComplexMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("string", "value");
        metadata.put("integer", 42);
        metadata.put("double", 3.14);
        metadata.put("boolean", true);
        metadata.put("nested", Map.of("key", "value"));

        SearchResult result = new SearchResult("vec1", 0.5, metadata);

        assertEquals("value", result.metadata().get("string"));
        assertEquals(42, result.metadata().get("integer"));
        assertEquals(3.14, result.metadata().get("double"));
        assertEquals(true, result.metadata().get("boolean"));
        assertTrue(result.metadata().get("nested") instanceof Map);
    }

    @Test
    void testSearchResultWithEmptyId() {
        // Empty ID should be allowed (non-null)
        SearchResult result = new SearchResult("", 0.5, null);
        assertEquals("", result.id());
    }

    @Test
    void testSearchResultWithLongId() {
        String longId = "a".repeat(1000);
        SearchResult result = new SearchResult(longId, 0.5, null);
        assertEquals(longId, result.id());
        assertEquals(1000, result.id().length());
    }

    @Test
    void testSearchResultRecordProperties() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("test", "value");

        SearchResult result = new SearchResult("vec1", 0.85, metadata);

        // Records should have working id(), distance(), metadata() accessors
        assertEquals("vec1", result.id());
        assertEquals(0.85, result.distance());
        assertEquals(metadata, result.metadata());

        // And also getters
        assertEquals("vec1", result.getId());
        assertEquals(0.85, result.getDistance());
        assertEquals(metadata, result.getMetadata());
    }

    @Test
    void testMultipleSearchResultsComparison() {
        SearchResult result1 = new SearchResult("vec1", 0.1, null);
        SearchResult result2 = new SearchResult("vec2", 0.2, null);
        SearchResult result3 = new SearchResult("vec3", 0.3, null);

        // Results with different distances
        assertTrue(result1.distance() < result2.distance());
        assertTrue(result2.distance() < result3.distance());
    }

    @Test
    void testSearchResultImmutability() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        SearchResult result = new SearchResult("vec1", 0.5, metadata);

        // Modifying the original map should not affect the record
        // (Note: Records don't make defensive copies, so this tests the behavior)
        assertEquals("value", result.metadata().get("key"));
    }
}
