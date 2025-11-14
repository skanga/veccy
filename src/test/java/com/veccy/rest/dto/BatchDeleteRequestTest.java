package com.veccy.rest.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for BatchDeleteRequest DTO.
 */
class BatchDeleteRequestTest {

    @Test
    void testDefaultConstructor() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        assertNull(request.getIds());
    }

    @Test
    void testSetAndGetIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = Arrays.asList("vec1", "vec2", "vec3");
        request.setIds(ids);

        assertEquals(ids, request.getIds());
        assertEquals(3, request.getIds().size());
    }

    @Test
    void testWithNullIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();
        request.setIds(null);

        assertNull(request.getIds());
    }

    @Test
    void testWithEmptyIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();
        request.setIds(new ArrayList<>());

        assertTrue(request.getIds().isEmpty());
    }

    @Test
    void testWithSingleId() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = Arrays.asList("vec1");
        request.setIds(ids);

        assertEquals(1, request.getIds().size());
        assertEquals("vec1", request.getIds().get(0));
    }

    @Test
    void testWithMultipleIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = Arrays.asList("vec1", "vec2", "vec3", "vec4", "vec5");
        request.setIds(ids);

        assertEquals(5, request.getIds().size());
    }

    @Test
    void testWithDuplicateIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = Arrays.asList("vec1", "vec1", "vec2", "vec1");
        request.setIds(ids);

        assertEquals(4, request.getIds().size());
        // Duplicates are allowed in the list
    }

    @Test
    void testWithEmptyStringIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = Arrays.asList("", "vec1", "");
        request.setIds(ids);

        assertEquals(3, request.getIds().size());
        assertTrue(request.getIds().contains(""));
    }

    @Test
    void testWithLongIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        String longId = "a".repeat(1000);
        List<String> ids = Arrays.asList(longId, "vec2");
        request.setIds(ids);

        assertEquals(2, request.getIds().size());
        assertEquals(1000, request.getIds().get(0).length());
    }

    @Test
    void testWithSpecialCharactersInIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = Arrays.asList(
                "vec-1",
                "vec_2",
                "vec.3",
                "vec:4",
                "vec@5"
        );
        request.setIds(ids);

        assertEquals(5, request.getIds().size());
        assertTrue(request.getIds().contains("vec-1"));
        assertTrue(request.getIds().contains("vec_2"));
    }

    @Test
    void testWithUnicodeIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = Arrays.asList("vec_日本語", "vec_中文", "vec_한글");
        request.setIds(ids);

        assertEquals(3, request.getIds().size());
        assertTrue(request.getIds().contains("vec_日本語"));
    }

    @Test
    void testWithNumericIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = Arrays.asList("1", "2", "3", "100", "999");
        request.setIds(ids);

        assertEquals(5, request.getIds().size());
        assertTrue(request.getIds().contains("1"));
    }

    @Test
    void testMultipleSetCalls() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids1 = Arrays.asList("vec1", "vec2");
        List<String> ids2 = Arrays.asList("vec3", "vec4", "vec5");

        request.setIds(ids1);
        request.setIds(ids2);

        assertEquals(ids2, request.getIds());
        assertEquals(3, request.getIds().size());
    }

    @Test
    void testWithLargeNumberOfIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            ids.add("vec" + i);
        }

        request.setIds(ids);

        assertEquals(10000, request.getIds().size());
    }

    @Test
    void testSetterReturnsVoid() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        // Verify setter works without returning anything
        request.setIds(Arrays.asList("vec1"));

        assertNotNull(request.getIds());
    }

    @Test
    void testWithMixedCaseIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = Arrays.asList("VEC1", "vec2", "Vec3", "VeC4");
        request.setIds(ids);

        assertEquals(4, request.getIds().size());
        assertTrue(request.getIds().contains("VEC1"));
        assertTrue(request.getIds().contains("vec2"));
    }

    @Test
    void testWithWhitespaceInIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = Arrays.asList("vec 1", " vec2", "vec3 ", " vec4 ");
        request.setIds(ids);

        assertEquals(4, request.getIds().size());
        assertTrue(request.getIds().contains("vec 1"));
        assertTrue(request.getIds().contains(" vec2"));
    }

    @Test
    void testIdsListMutability() {
        BatchDeleteRequest request = new BatchDeleteRequest();

        List<String> ids = new ArrayList<>(Arrays.asList("vec1", "vec2"));
        request.setIds(ids);

        // Get the list and verify it's the same reference
        List<String> retrievedIds = request.getIds();
        assertEquals(ids, retrievedIds);
    }
}
