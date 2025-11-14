package com.veccy.rest.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PaginatedResponse and PaginationMetadata.
 */
class PaginatedResponseTest {

    @Test
    void testConstructor() {
        List<String> data = List.of("item1", "item2", "item3");
        PaginatedResponse.PaginationMetadata metadata =
            new PaginatedResponse.PaginationMetadata(1, 10, 100);

        PaginatedResponse<String> response = new PaginatedResponse<>(data, metadata);

        assertNotNull(response);
        assertEquals(data, response.getData());
        assertEquals(metadata, response.getPagination());
    }

    @Test
    void testOfFactoryMethod() {
        List<Integer> data = List.of(1, 2, 3, 4, 5);
        PaginatedResponse<Integer> response = PaginatedResponse.of(data, 2, 5, 25);

        assertNotNull(response);
        assertEquals(5, response.getData().size());
        assertEquals(2, response.getPagination().getPage());
        assertEquals(5, response.getPagination().getPageSize());
        assertEquals(25, response.getPagination().getTotalItems());
    }

    @Test
    void testPaginationMetadataBasicFields() {
        PaginatedResponse.PaginationMetadata metadata =
            new PaginatedResponse.PaginationMetadata(3, 20, 100);

        assertEquals(3, metadata.getPage());
        assertEquals(20, metadata.getPageSize());
        assertEquals(100, metadata.getTotalItems());
    }

    @Test
    void testTotalPagesCalculation() {
        // Exact division
        PaginatedResponse.PaginationMetadata metadata1 =
            new PaginatedResponse.PaginationMetadata(1, 10, 100);
        assertEquals(10, metadata1.getTotalPages());

        // With remainder
        PaginatedResponse.PaginationMetadata metadata2 =
            new PaginatedResponse.PaginationMetadata(1, 10, 95);
        assertEquals(10, metadata2.getTotalPages()); // Ceiling: 95/10 = 10

        // Single page
        PaginatedResponse.PaginationMetadata metadata3 =
            new PaginatedResponse.PaginationMetadata(1, 20, 15);
        assertEquals(1, metadata3.getTotalPages());

        // Empty
        PaginatedResponse.PaginationMetadata metadata4 =
            new PaginatedResponse.PaginationMetadata(1, 20, 0);
        assertEquals(0, metadata4.getTotalPages());
    }

    @Test
    void testHasNext() {
        // First page with more pages
        PaginatedResponse.PaginationMetadata metadata1 =
            new PaginatedResponse.PaginationMetadata(1, 10, 100);
        assertTrue(metadata1.isHasNext());

        // Middle page
        PaginatedResponse.PaginationMetadata metadata2 =
            new PaginatedResponse.PaginationMetadata(5, 10, 100);
        assertTrue(metadata2.isHasNext());

        // Last page
        PaginatedResponse.PaginationMetadata metadata3 =
            new PaginatedResponse.PaginationMetadata(10, 10, 100);
        assertFalse(metadata3.isHasNext());

        // Beyond last page
        PaginatedResponse.PaginationMetadata metadata4 =
            new PaginatedResponse.PaginationMetadata(11, 10, 100);
        assertFalse(metadata4.isHasNext());

        // Single page
        PaginatedResponse.PaginationMetadata metadata5 =
            new PaginatedResponse.PaginationMetadata(1, 20, 10);
        assertFalse(metadata5.isHasNext());
    }

    @Test
    void testHasPrevious() {
        // First page
        PaginatedResponse.PaginationMetadata metadata1 =
            new PaginatedResponse.PaginationMetadata(1, 10, 100);
        assertFalse(metadata1.isHasPrevious());

        // Second page
        PaginatedResponse.PaginationMetadata metadata2 =
            new PaginatedResponse.PaginationMetadata(2, 10, 100);
        assertTrue(metadata2.isHasPrevious());

        // Middle page
        PaginatedResponse.PaginationMetadata metadata3 =
            new PaginatedResponse.PaginationMetadata(5, 10, 100);
        assertTrue(metadata3.isHasPrevious());

        // Last page
        PaginatedResponse.PaginationMetadata metadata4 =
            new PaginatedResponse.PaginationMetadata(10, 10, 100);
        assertTrue(metadata4.isHasPrevious());
    }

    @Test
    void testEmptyData() {
        List<String> emptyData = new ArrayList<>();
        PaginatedResponse<String> response = PaginatedResponse.of(emptyData, 1, 20, 0);

        assertTrue(response.getData().isEmpty());
        assertEquals(0, response.getPagination().getTotalPages());
        assertFalse(response.getPagination().isHasNext());
        assertFalse(response.getPagination().isHasPrevious());
    }

    @Test
    void testSingleItemSinglePage() {
        List<String> data = List.of("only-item");
        PaginatedResponse<String> response = PaginatedResponse.of(data, 1, 20, 1);

        assertEquals(1, response.getData().size());
        assertEquals(1, response.getPagination().getTotalPages());
        assertFalse(response.getPagination().isHasNext());
        assertFalse(response.getPagination().isHasPrevious());
    }

    @Test
    void testLargeDataset() {
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            data.add(i);
        }

        PaginatedResponse<Integer> response = PaginatedResponse.of(data, 1, 100, 10000);

        assertEquals(100, response.getData().size());
        assertEquals(100, response.getPagination().getTotalPages());
        assertTrue(response.getPagination().isHasNext());
    }

    @Test
    void testPageNavigationScenario() {
        // First page
        PaginatedResponse<String> page1 = PaginatedResponse.of(
            List.of("item1", "item2"), 1, 2, 10);
        assertFalse(page1.getPagination().isHasPrevious());
        assertTrue(page1.getPagination().isHasNext());

        // Middle page
        PaginatedResponse<String> page3 = PaginatedResponse.of(
            List.of("item5", "item6"), 3, 2, 10);
        assertTrue(page3.getPagination().isHasPrevious());
        assertTrue(page3.getPagination().isHasNext());

        // Last page
        PaginatedResponse<String> page5 = PaginatedResponse.of(
            List.of("item9", "item10"), 5, 2, 10);
        assertTrue(page5.getPagination().isHasPrevious());
        assertFalse(page5.getPagination().isHasNext());
    }

    @Test
    void testPartialLastPage() {
        // 95 items with page size 20 = 5 pages (last page has 15 items)
        PaginatedResponse<Integer> lastPage = PaginatedResponse.of(
            List.of(81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95),
            5, 20, 95);

        assertEquals(15, lastPage.getData().size());
        assertEquals(5, lastPage.getPagination().getTotalPages());
        assertFalse(lastPage.getPagination().isHasNext());
        assertTrue(lastPage.getPagination().isHasPrevious());
    }

    @Test
    void testDifferentGenericTypes() {
        // String type
        PaginatedResponse<String> stringResponse =
            PaginatedResponse.of(List.of("a", "b"), 1, 2, 10);
        assertInstanceOf(String.class, stringResponse.getData().get(0));

        // Integer type
        PaginatedResponse<Integer> intResponse =
            PaginatedResponse.of(List.of(1, 2), 1, 2, 10);
        assertInstanceOf(Integer.class, intResponse.getData().get(0));

        // Custom object type
        record TestObject(String name, int id) {}
        PaginatedResponse<TestObject> objectResponse =
            PaginatedResponse.of(List.of(new TestObject("test", 1)), 1, 1, 1);
        assertInstanceOf(TestObject.class, objectResponse.getData().get(0));
    }

    @Test
    void testPageBoundaries() {
        // Page 1 of 1 (only page)
        PaginatedResponse.PaginationMetadata onlyPage =
            new PaginatedResponse.PaginationMetadata(1, 20, 10);
        assertFalse(onlyPage.isHasPrevious());
        assertFalse(onlyPage.isHasNext());

        // Page 1 of many
        PaginatedResponse.PaginationMetadata firstPage =
            new PaginatedResponse.PaginationMetadata(1, 20, 1000);
        assertFalse(firstPage.isHasPrevious());
        assertTrue(firstPage.isHasNext());

        // Last page of many
        PaginatedResponse.PaginationMetadata lastPage =
            new PaginatedResponse.PaginationMetadata(50, 20, 1000);
        assertTrue(lastPage.isHasPrevious());
        assertFalse(lastPage.isHasNext());
    }

    @Test
    void testVeryLargeDataset() {
        PaginatedResponse.PaginationMetadata metadata =
            new PaginatedResponse.PaginationMetadata(1, 100, 1000000);

        assertEquals(10000, metadata.getTotalPages());
        assertTrue(metadata.isHasNext());
        assertFalse(metadata.isHasPrevious());
    }

    @Test
    void testNullDataHandling() {
        PaginatedResponse.PaginationMetadata metadata =
            new PaginatedResponse.PaginationMetadata(1, 20, 0);
        PaginatedResponse<String> response = new PaginatedResponse<>(null, metadata);

        assertNull(response.getData());
        assertNotNull(response.getPagination());
    }

    @Test
    void testPageSizeOne() {
        // Each page has exactly one item
        PaginatedResponse.PaginationMetadata metadata =
            new PaginatedResponse.PaginationMetadata(5, 1, 100);

        assertEquals(100, metadata.getTotalPages());
        assertTrue(metadata.isHasNext());
        assertTrue(metadata.isHasPrevious());
    }
}
