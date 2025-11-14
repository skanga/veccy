package com.veccy.rest.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiResponse.
 */
class ApiResponseTest {

    @Test
    void testDefaultConstructor() {
        ApiResponse<String> response = new ApiResponse<>();

        assertNotNull(response);
        assertNotNull(response.getTimestamp());
        assertFalse(response.isSuccess()); // default false
    }

    @Test
    void testFullConstructor() {
        String data = "test data";
        ApiResponse<String> response = new ApiResponse<>(true, "Success message", data);

        assertTrue(response.isSuccess());
        assertEquals("Success message", response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testSuccessWithData() {
        String data = "operation successful";
        ApiResponse<String> response = ApiResponse.success(data);

        assertTrue(response.isSuccess());
        assertEquals("Success", response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testSuccessWithMessageAndData() {
        String message = "Database created successfully";
        Map<String, Object> data = Map.of("id", "db123", "name", "test_db");
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);

        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testError() {
        String errorMessage = "Database not found";
        ApiResponse<String> response = ApiResponse.error(errorMessage);

        assertFalse(response.isSuccess());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testSettersAndGetters() {
        ApiResponse<Integer> response = new ApiResponse<>();

        response.setSuccess(true);
        assertTrue(response.isSuccess());

        response.setMessage("Test message");
        assertEquals("Test message", response.getMessage());

        response.setData(42);
        assertEquals(42, response.getData());

        Long timestamp = System.currentTimeMillis();
        response.setTimestamp(timestamp);
        assertEquals(timestamp, response.getTimestamp());
    }

    @Test
    void testWithNullData() {
        ApiResponse<String> response = new ApiResponse<>(true, "Success", null);

        assertTrue(response.isSuccess());
        assertEquals("Success", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testWithNullMessage() {
        ApiResponse<String> response = new ApiResponse<>(true, null, "data");

        assertTrue(response.isSuccess());
        assertNull(response.getMessage());
        assertEquals("data", response.getData());
    }

    @Test
    void testWithComplexDataType() {
        List<String> data = List.of("item1", "item2", "item3");
        ApiResponse<List<String>> response = ApiResponse.success("List created", data);

        assertTrue(response.isSuccess());
        assertEquals(3, response.getData().size());
        assertTrue(response.getData().contains("item1"));
    }

    @Test
    void testTimestampIsRecent() throws InterruptedException {
        long before = System.currentTimeMillis();
        Thread.sleep(1); // Ensure some time passes
        ApiResponse<String> response = new ApiResponse<>(true, "test", "data");
        long after = System.currentTimeMillis();

        assertNotNull(response.getTimestamp());
        assertTrue(response.getTimestamp() >= before);
        assertTrue(response.getTimestamp() <= after);
    }

    @Test
    void testMultipleResponsesHaveDifferentTimestamps() throws InterruptedException {
        ApiResponse<String> response1 = new ApiResponse<>();
        Thread.sleep(2);
        ApiResponse<String> response2 = new ApiResponse<>();

        assertNotEquals(response1.getTimestamp(), response2.getTimestamp());
    }

    @Test
    void testGenericTypeWithMap() {
        Map<String, Object> data = Map.of(
            "vectors", List.of(1.0, 2.0, 3.0),
            "metadata", Map.of("key", "value"),
            "count", 100
        );

        ApiResponse<Map<String, Object>> response = ApiResponse.success(data);

        assertTrue(response.isSuccess());
        assertEquals(3, response.getData().size());
        assertEquals(100, response.getData().get("count"));
    }

    @Test
    void testErrorWithEmptyMessage() {
        ApiResponse<String> response = ApiResponse.error("");

        assertFalse(response.isSuccess());
        assertEquals("", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testSuccessModifiedToError() {
        ApiResponse<String> response = ApiResponse.success("initial data");

        assertTrue(response.isSuccess());

        response.setSuccess(false);
        response.setMessage("Something went wrong");
        response.setData(null);

        assertFalse(response.isSuccess());
        assertEquals("Something went wrong", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testWithLongMessage() {
        String longMessage = "a".repeat(1000);
        ApiResponse<String> response = ApiResponse.error(longMessage);

        assertFalse(response.isSuccess());
        assertEquals(1000, response.getMessage().length());
    }

    @Test
    void testWithSpecialCharacters() {
        String message = "Error: \"quoted\" & <special> chars\n\ttab";
        ApiResponse<String> response = ApiResponse.error(message);

        assertEquals(message, response.getMessage());
    }

    @Test
    void testTypeParameter() {
        // Test with different generic types
        ApiResponse<String> stringResponse = ApiResponse.success("text");
        ApiResponse<Integer> intResponse = ApiResponse.success(123);
        ApiResponse<Boolean> boolResponse = ApiResponse.success(true);
        ApiResponse<Double> doubleResponse = ApiResponse.success(3.14);

        assertInstanceOf(String.class, stringResponse.getData());
        assertInstanceOf(Integer.class, intResponse.getData());
        assertInstanceOf(Boolean.class, boolResponse.getData());
        assertInstanceOf(Double.class, doubleResponse.getData());
    }
}
