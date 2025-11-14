package com.veccy.base;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for VectorWithMetadata record.
 */
class VectorWithMetadataTest {

    @Test
    void testVectorWithMetadataConstructor() {
        double[] vector = {1.0, 2.0, 3.0};
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");

        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, metadata);

        assertEquals("vec1", vwm.id());
        assertArrayEquals(vector, vwm.vector());
        assertEquals(metadata, vwm.metadata());
    }

    @Test
    void testVectorWithMetadataWithNullId() {
        double[] vector = {1.0, 2.0};
        assertThrows(NullPointerException.class, () -> {
            new VectorWithMetadata(null, vector, null);
        });
    }

    @Test
    void testVectorWithMetadataWithNullVector() {
        assertThrows(NullPointerException.class, () -> {
            new VectorWithMetadata("vec1", null, null);
        });
    }

    @Test
    void testVectorWithMetadataWithNullMetadata() {
        double[] vector = {1.0, 2.0, 3.0};
        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, null);

        assertEquals("vec1", vwm.id());
        assertArrayEquals(vector, vwm.vector());
        assertNull(vwm.metadata());
    }

    @Test
    void testGetId() {
        double[] vector = {1.0, 2.0};
        VectorWithMetadata vwm = new VectorWithMetadata("test_id", vector, null);
        assertEquals("test_id", vwm.getId());
    }

    @Test
    void testGetVector() {
        double[] vector = {1.5, 2.5, 3.5};
        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, null);
        assertArrayEquals(vector, vwm.getVector());
    }

    @Test
    void testGetMetadata() {
        double[] vector = {1.0, 2.0};
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, metadata);

        assertEquals(metadata, vwm.getMetadata());
        assertEquals("value", vwm.getMetadata().get("key"));
    }

    @Test
    void testEqualsBasedOnIdOnly() {
        double[] vector1 = {1.0, 2.0, 3.0};
        double[] vector2 = {4.0, 5.0, 6.0};

        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("label", "A");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("label", "B");

        VectorWithMetadata vwm1 = new VectorWithMetadata("vec1", vector1, metadata1);
        VectorWithMetadata vwm2 = new VectorWithMetadata("vec1", vector2, metadata2);

        // Should be equal because ID is the same, even though vector and metadata differ
        assertEquals(vwm1, vwm2);
    }

    @Test
    void testNotEqualsWithDifferentIds() {
        double[] vector = {1.0, 2.0, 3.0};

        VectorWithMetadata vwm1 = new VectorWithMetadata("vec1", vector, null);
        VectorWithMetadata vwm2 = new VectorWithMetadata("vec2", vector, null);

        assertNotEquals(vwm1, vwm2);
    }

    @Test
    void testHashCodeBasedOnIdOnly() {
        double[] vector1 = {1.0, 2.0, 3.0};
        double[] vector2 = {4.0, 5.0, 6.0};

        VectorWithMetadata vwm1 = new VectorWithMetadata("vec1", vector1, null);
        VectorWithMetadata vwm2 = new VectorWithMetadata("vec1", vector2, null);

        // Hash code should be equal because ID is the same
        assertEquals(vwm1.hashCode(), vwm2.hashCode());
    }

    @Test
    void testToStringShowsVectorDimension() {
        double[] vector = {1.0, 2.0, 3.0, 4.0, 5.0};
        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, null);

        String str = vwm.toString();
        assertNotNull(str);
        assertTrue(str.contains("vec1"));
        assertTrue(str.contains("vectorDim=5"));
        assertFalse(str.contains("1.0")); // Should not show full array
    }

    @Test
    void testToStringWithMetadata() {
        double[] vector = {1.0, 2.0};
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");

        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, metadata);

        String str = vwm.toString();
        assertTrue(str.contains("vec1"));
        assertTrue(str.contains("vectorDim=2"));
        assertTrue(str.contains("metadata"));
    }

    @Test
    void testWithEmptyVector() {
        double[] emptyVector = {};
        VectorWithMetadata vwm = new VectorWithMetadata("vec1", emptyVector, null);

        assertEquals("vec1", vwm.id());
        assertEquals(0, vwm.vector().length);
    }

    @Test
    void testWithSingleDimensionVector() {
        double[] vector = {42.0};
        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, null);

        assertEquals(1, vwm.vector().length);
        assertEquals(42.0, vwm.vector()[0]);
    }

    @Test
    void testWithHighDimensionalVector() {
        double[] vector = new double[1536]; // Common embedding dimension
        for (int i = 0; i < vector.length; i++) {
            vector[i] = i * 0.1;
        }

        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, null);

        assertEquals(1536, vwm.vector().length);
        assertEquals(0.0, vwm.vector()[0]);
        assertEquals(153.5, vwm.vector()[1535], 0.01);
    }

    @Test
    void testWithComplexMetadata() {
        double[] vector = {1.0, 2.0};
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("string", "value");
        metadata.put("integer", 42);
        metadata.put("double", 3.14);
        metadata.put("boolean", true);
        metadata.put("list", java.util.Arrays.asList("a", "b", "c"));

        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, metadata);

        assertEquals("value", vwm.metadata().get("string"));
        assertEquals(42, vwm.metadata().get("integer"));
        assertEquals(3.14, vwm.metadata().get("double"));
        assertEquals(true, vwm.metadata().get("boolean"));
        assertTrue(vwm.metadata().get("list") instanceof java.util.List);
    }

    @Test
    void testWithEmptyMetadata() {
        double[] vector = {1.0, 2.0};
        Map<String, Object> emptyMetadata = new HashMap<>();

        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, emptyMetadata);

        assertTrue(vwm.metadata().isEmpty());
    }

    @Test
    void testWithEmptyId() {
        double[] vector = {1.0, 2.0};
        // Empty ID should be allowed (non-null)
        VectorWithMetadata vwm = new VectorWithMetadata("", vector, null);
        assertEquals("", vwm.id());
    }

    @Test
    void testWithLongId() {
        double[] vector = {1.0, 2.0};
        String longId = "a".repeat(1000);

        VectorWithMetadata vwm = new VectorWithMetadata(longId, vector, null);
        assertEquals(longId, vwm.id());
    }

    @Test
    void testRecordAccessors() {
        double[] vector = {1.0, 2.0, 3.0};
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, metadata);

        // Test record accessors
        assertEquals("vec1", vwm.id());
        assertArrayEquals(vector, vwm.vector());
        assertEquals(metadata, vwm.metadata());

        // Test getter methods
        assertEquals("vec1", vwm.getId());
        assertArrayEquals(vector, vwm.getVector());
        assertEquals(metadata, vwm.getMetadata());
    }

    @Test
    void testEqualsWithSameInstance() {
        double[] vector = {1.0, 2.0};
        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, null);

        assertEquals(vwm, vwm);
    }

    @Test
    void testEqualsWithNull() {
        double[] vector = {1.0, 2.0};
        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, null);

        assertNotEquals(vwm, null);
    }

    @Test
    void testEqualsWithDifferentClass() {
        double[] vector = {1.0, 2.0};
        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, null);

        assertNotEquals(vwm, "not a VectorWithMetadata");
    }

    @Test
    void testVectorWithSpecialDoubleValues() {
        double[] vector = {
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE,
                Double.MIN_VALUE,
                0.0,
                -0.0
        };

        VectorWithMetadata vwm = new VectorWithMetadata("vec1", vector, null);

        assertEquals(7, vwm.vector().length);
        assertTrue(Double.isNaN(vwm.vector()[0]));
        assertEquals(Double.POSITIVE_INFINITY, vwm.vector()[1]);
        assertEquals(Double.NEGATIVE_INFINITY, vwm.vector()[2]);
    }

    @Test
    void testMultipleInstancesWithSameId() {
        double[] vector1 = {1.0, 2.0};
        double[] vector2 = {3.0, 4.0};

        VectorWithMetadata vwm1 = new VectorWithMetadata("same_id", vector1, null);
        VectorWithMetadata vwm2 = new VectorWithMetadata("same_id", vector2, null);
        VectorWithMetadata vwm3 = new VectorWithMetadata("same_id", vector1, null);

        // All should be equal based on ID
        assertEquals(vwm1, vwm2);
        assertEquals(vwm2, vwm3);
        assertEquals(vwm1, vwm3);

        // Hash codes should be equal
        assertEquals(vwm1.hashCode(), vwm2.hashCode());
        assertEquals(vwm2.hashCode(), vwm3.hashCode());
    }
}
