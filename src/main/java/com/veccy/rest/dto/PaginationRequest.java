package com.veccy.rest.dto;

/**
 * Request parameters for pagination.
 */
public class PaginationRequest {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private int page = 1;      // 1-based page number
    private int pageSize = DEFAULT_PAGE_SIZE;

    public PaginationRequest() {
    }

    public PaginationRequest(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * Get validated page number (minimum 1).
     */
    public int getPage() {
        return Math.max(1, page);
    }

    public void setPage(int page) {
        this.page = page;
    }

    /**
     * Get validated page size (between 1 and MAX_PAGE_SIZE).
     */
    public int getPageSize() {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Calculate offset for database queries.
     */
    public int getOffset() {
        return (getPage() - 1) * getPageSize();
    }

    /**
     * Calculate limit for database queries.
     */
    public int getLimit() {
        return getPageSize();
    }

    public static int getMaxPageSize() {
        return MAX_PAGE_SIZE;
    }

    public static int getDefaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }
}
