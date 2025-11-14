package com.veccy.rest.dto;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * @param <T> The type of data being paginated
 */
public class PaginatedResponse<T> {
    private final List<T> data;
    private final PaginationMetadata pagination;

    public PaginatedResponse(List<T> data, PaginationMetadata pagination) {
        this.data = data;
        this.pagination = pagination;
    }

    public List<T> getData() {
        return data;
    }

    public PaginationMetadata getPagination() {
        return pagination;
    }

    /**
     * Metadata about the pagination.
     */
    public static class PaginationMetadata {
        private final int page;
        private final int pageSize;
        private final long totalItems;
        private final int totalPages;
        private final boolean hasNext;
        private final boolean hasPrevious;

        public PaginationMetadata(int page, int pageSize, long totalItems) {
            this.page = page;
            this.pageSize = pageSize;
            this.totalItems = totalItems;
            this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
            this.hasNext = page < totalPages;
            this.hasPrevious = page > 1;
        }

        public int getPage() {
            return page;
        }

        public int getPageSize() {
            return pageSize;
        }

        public long getTotalItems() {
            return totalItems;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public boolean isHasNext() {
            return hasNext;
        }

        public boolean isHasPrevious() {
            return hasPrevious;
        }
    }

    /**
     * Create a paginated response from data and pagination parameters.
     */
    public static <T> PaginatedResponse<T> of(List<T> data, int page, int pageSize, long totalItems) {
        PaginationMetadata metadata = new PaginationMetadata(page, pageSize, totalItems);
        return new PaginatedResponse<>(data, metadata);
    }
}
