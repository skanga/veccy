package com.veccy.base;

import java.util.List;
import java.util.Optional;

/**
 * Represents a page of results with pagination support.
 * <p>
 * This record provides cursor-based pagination for efficient iteration over large result sets.
 * Cursor-based pagination is more efficient than offset-based pagination for large datasets
 * and provides consistent results even when data is being modified.
 *
 * @param <T> the type of items in this page
 * @param items the list of items in this page
 * @param nextCursor optional cursor to fetch the next page (empty if this is the last page)
 * @param hasMore true if there are more items after this page
 */
public record Page<T>(
        List<T> items,
        Optional<String> nextCursor,
        boolean hasMore
) {
    /**
     * Create a page with items and a next cursor.
     *
     * @param items the items in this page
     * @param nextCursor cursor for the next page
     * @param <T> the type of items
     * @return a new Page instance
     */
    public static <T> Page<T> of(List<T> items, String nextCursor) {
        return new Page<>(items, Optional.of(nextCursor), true);
    }

    /**
     * Create a final page (no more items after this).
     *
     * @param items the items in this page
     * @param <T> the type of items
     * @return a new Page instance with no next cursor
     */
    public static <T> Page<T> last(List<T> items) {
        return new Page<>(items, Optional.empty(), false);
    }

    /**
     * Create an empty page.
     *
     * @param <T> the type of items
     * @return an empty Page instance
     */
    public static <T> Page<T> empty() {
        return new Page<>(List.of(), Optional.empty(), false);
    }

    /**
     * Get the number of items in this page.
     *
     * @return the size of the items list
     */
    public int size() {
        return items.size();
    }

    /**
     * Check if this page is empty.
     *
     * @return true if there are no items in this page
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
}
