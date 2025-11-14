package com.veccy.rest.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PaginationRequest.
 */
class PaginationRequestTest {

    @Test
    void testDefaultConstructor() {
        PaginationRequest request = new PaginationRequest();

        assertEquals(1, request.getPage());
        assertEquals(20, request.getPageSize()); // Default page size
    }

    @Test
    void testConstructorWithParameters() {
        PaginationRequest request = new PaginationRequest(5, 50);

        assertEquals(5, request.getPage());
        assertEquals(50, request.getPageSize());
    }

    @Test
    void testSetAndGetPage() {
        PaginationRequest request = new PaginationRequest();

        request.setPage(10);
        assertEquals(10, request.getPage());
    }

    @Test
    void testSetAndGetPageSize() {
        PaginationRequest request = new PaginationRequest();

        request.setPageSize(30);
        assertEquals(30, request.getPageSize());
    }

    @Test
    void testPageMinimumIsOne() {
        PaginationRequest request = new PaginationRequest();

        request.setPage(0);
        assertEquals(1, request.getPage()); // Minimum is 1

        request.setPage(-5);
        assertEquals(1, request.getPage()); // Negative becomes 1
    }

    @Test
    void testPageSizeMaximum() {
        PaginationRequest request = new PaginationRequest();

        request.setPageSize(200);
        assertEquals(100, request.getPageSize()); // Max is 100

        request.setPageSize(500);
        assertEquals(100, request.getPageSize());
    }

    @Test
    void testPageSizeMinimum() {
        PaginationRequest request = new PaginationRequest();

        request.setPageSize(0);
        assertEquals(20, request.getPageSize()); // Returns default

        request.setPageSize(-10);
        assertEquals(20, request.getPageSize()); // Returns default
    }

    @Test
    void testGetOffset() {
        PaginationRequest request = new PaginationRequest();

        request.setPage(1);
        request.setPageSize(20);
        assertEquals(0, request.getOffset()); // Page 1, offset 0

        request.setPage(2);
        assertEquals(20, request.getOffset()); // Page 2, offset 20

        request.setPage(3);
        assertEquals(40, request.getOffset()); // Page 3, offset 40

        request.setPage(5);
        request.setPageSize(50);
        assertEquals(200, request.getOffset()); // Page 5, size 50, offset 200
    }

    @Test
    void testGetLimit() {
        PaginationRequest request = new PaginationRequest();

        request.setPageSize(25);
        assertEquals(25, request.getLimit());

        request.setPageSize(100);
        assertEquals(100, request.getLimit());
    }

    @Test
    void testGetMaxPageSize() {
        assertEquals(100, PaginationRequest.getMaxPageSize());
    }

    @Test
    void testGetDefaultPageSize() {
        assertEquals(20, PaginationRequest.getDefaultPageSize());
    }

    @Test
    void testOffsetCalculationForVariousPages() {
        PaginationRequest request = new PaginationRequest();
        request.setPageSize(10);

        request.setPage(1);
        assertEquals(0, request.getOffset());

        request.setPage(10);
        assertEquals(90, request.getOffset());

        request.setPage(100);
        assertEquals(990, request.getOffset());
    }

    @Test
    void testPageSizeAtBoundaries() {
        PaginationRequest request = new PaginationRequest();

        request.setPageSize(1);
        assertEquals(1, request.getPageSize());

        request.setPageSize(100);
        assertEquals(100, request.getPageSize());

        request.setPageSize(101);
        assertEquals(100, request.getPageSize()); // Capped at max
    }

    @Test
    void testConstructorWithInvalidValues() {
        PaginationRequest request = new PaginationRequest(0, 0);

        assertEquals(1, request.getPage()); // Fixed to minimum
        assertEquals(20, request.getPageSize()); // Fixed to default
    }

    @Test
    void testConstructorWithExcessiveValues() {
        PaginationRequest request = new PaginationRequest(1000, 500);

        assertEquals(1000, request.getPage());
        assertEquals(100, request.getPageSize()); // Capped at max
    }

    @Test
    void testOffsetWithNegativePage() {
        PaginationRequest request = new PaginationRequest();
        request.setPage(-1);
        request.setPageSize(20);

        assertEquals(0, request.getOffset()); // Page normalized to 1, so offset is 0
    }

    @Test
    void testMultipleUpdates() {
        PaginationRequest request = new PaginationRequest();

        request.setPage(5);
        request.setPageSize(25);
        assertEquals(100, request.getOffset());

        request.setPage(10);
        assertEquals(225, request.getOffset());

        request.setPageSize(50);
        assertEquals(450, request.getOffset());
    }

    @Test
    void testLargePageNumber() {
        PaginationRequest request = new PaginationRequest();
        request.setPage(10000);
        request.setPageSize(20);

        // Should calculate correctly without overflow
        assertEquals(199980, request.getOffset());
    }

    @Test
    void testTypicalPaginationScenarios() {
        // First page
        PaginationRequest firstPage = new PaginationRequest(1, 20);
        assertEquals(0, firstPage.getOffset());
        assertEquals(20, firstPage.getLimit());

        // Middle page
        PaginationRequest middlePage = new PaginationRequest(5, 20);
        assertEquals(80, middlePage.getOffset());
        assertEquals(20, middlePage.getLimit());

        // Custom page size
        PaginationRequest customSize = new PaginationRequest(3, 50);
        assertEquals(100, customSize.getOffset());
        assertEquals(50, customSize.getLimit());
    }
}
