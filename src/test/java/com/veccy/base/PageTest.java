package com.veccy.base;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Page record.
 */
class PageTest {

    @Test
    void testPageConstructor() {
        List<String> items = Arrays.asList("a", "b", "c");
        Optional<String> cursor = Optional.of("cursor1");

        Page<String> page = new Page<>(items, cursor, true);

        assertEquals(items, page.items());
        assertEquals(cursor, page.nextCursor());
        assertTrue(page.hasMore());
    }

    @Test
    void testPageOf() {
        List<String> items = Arrays.asList("item1", "item2");
        String cursor = "next_cursor";

        Page<String> page = Page.of(items, cursor);

        assertEquals(items, page.items());
        assertEquals(Optional.of(cursor), page.nextCursor());
        assertTrue(page.hasMore());
    }

    @Test
    void testPageLast() {
        List<String> items = Arrays.asList("final1", "final2");

        Page<String> page = Page.last(items);

        assertEquals(items, page.items());
        assertEquals(Optional.empty(), page.nextCursor());
        assertFalse(page.hasMore());
    }

    @Test
    void testPageEmpty() {
        Page<String> page = Page.empty();

        assertTrue(page.items().isEmpty());
        assertEquals(Optional.empty(), page.nextCursor());
        assertFalse(page.hasMore());
    }

    @Test
    void testSize() {
        Page<String> page1 = Page.of(Arrays.asList("a", "b", "c"), "cursor");
        assertEquals(3, page1.size());

        Page<String> page2 = Page.empty();
        assertEquals(0, page2.size());
    }

    @Test
    void testIsEmpty() {
        Page<String> emptyPage = Page.empty();
        assertTrue(emptyPage.isEmpty());

        Page<String> nonEmptyPage = Page.of(Arrays.asList("a"), "cursor");
        assertFalse(nonEmptyPage.isEmpty());
    }

    @Test
    void testPageWithEmptyList() {
        Page<String> page = Page.last(List.of());

        assertTrue(page.isEmpty());
        assertEquals(0, page.size());
        assertFalse(page.hasMore());
    }

    @Test
    void testPageWithSingleItem() {
        Page<Integer> page = Page.of(Arrays.asList(42), "cursor");

        assertEquals(1, page.size());
        assertEquals(42, page.items().get(0));
        assertTrue(page.hasMore());
    }

    @Test
    void testPageWithDifferentTypes() {
        // Test with Integer
        Page<Integer> intPage = Page.of(Arrays.asList(1, 2, 3), "cursor");
        assertEquals(3, intPage.size());

        // Test with Double
        Page<Double> doublePage = Page.of(Arrays.asList(1.5, 2.5), "cursor");
        assertEquals(2, doublePage.size());

        // Test with custom object
        Page<List<String>> listPage = Page.of(Arrays.asList(
                Arrays.asList("a", "b"),
                Arrays.asList("c", "d")
        ), "cursor");
        assertEquals(2, listPage.size());
    }

    @Test
    void testPageImmutability() {
        List<String> items = Arrays.asList("a", "b");
        Page<String> page = Page.of(items, "cursor");

        // Items should be accessible
        assertEquals(2, page.items().size());

        // Original list is separate
        assertEquals("a", page.items().get(0));
    }

    @Test
    void testPageEquals() {
        List<String> items1 = Arrays.asList("a", "b");
        List<String> items2 = Arrays.asList("a", "b");

        Page<String> page1 = Page.of(items1, "cursor");
        Page<String> page2 = Page.of(items2, "cursor");

        assertEquals(page1, page2);
    }

    @Test
    void testPageHashCode() {
        List<String> items = Arrays.asList("a", "b");

        Page<String> page1 = Page.of(items, "cursor");
        Page<String> page2 = Page.of(items, "cursor");

        assertEquals(page1.hashCode(), page2.hashCode());
    }

    @Test
    void testPageToString() {
        Page<String> page = Page.of(Arrays.asList("a", "b"), "cursor");

        String str = page.toString();
        assertNotNull(str);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
    }

    @Test
    void testPageWithNullCursor() {
        List<String> items = Arrays.asList("a", "b");
        Page<String> page = new Page<>(items, Optional.empty(), false);

        assertEquals(Optional.empty(), page.nextCursor());
        assertFalse(page.hasMore());
    }

    @Test
    void testPageHasMoreWithoutCursor() {
        // A page can have hasMore=true even with a cursor
        List<String> items = Arrays.asList("a", "b");
        Page<String> page = new Page<>(items, Optional.of("cursor"), true);

        assertTrue(page.hasMore());
        assertTrue(page.nextCursor().isPresent());
    }

    @Test
    void testPageHasMoreFalseWithCursor() {
        // Edge case: hasMore=false but cursor present (should not happen in practice)
        List<String> items = Arrays.asList("a", "b");
        Page<String> page = new Page<>(items, Optional.of("cursor"), false);

        assertFalse(page.hasMore());
        assertTrue(page.nextCursor().isPresent());
    }

    @Test
    void testEmptyPageStaticFactory() {
        Page<Integer> page1 = Page.empty();
        Page<String> page2 = Page.empty();

        // Both should be empty regardless of type
        assertTrue(page1.isEmpty());
        assertTrue(page2.isEmpty());
    }

    @Test
    void testLargePageSize() {
        List<Integer> largeList = new java.util.ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeList.add(i);
        }

        Page<Integer> page = Page.of(largeList, "cursor");

        assertEquals(10000, page.size());
        assertFalse(page.isEmpty());
    }

    @Test
    void testPageAccessors() {
        List<String> items = Arrays.asList("x", "y", "z");
        String cursor = "test_cursor";
        boolean hasMore = true;

        Page<String> page = new Page<>(items, Optional.of(cursor), hasMore);

        // Test all accessors
        assertSame(items, page.items());
        assertEquals(Optional.of(cursor), page.nextCursor());
        assertEquals(hasMore, page.hasMore());
        assertEquals(3, page.size());
        assertFalse(page.isEmpty());
    }
}
