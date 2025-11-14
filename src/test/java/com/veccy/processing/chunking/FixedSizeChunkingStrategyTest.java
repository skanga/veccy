package com.veccy.processing.chunking;

import com.veccy.processing.parsers.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for FixedSizeChunkingStrategy.
 */
class FixedSizeChunkingStrategyTest {

    private FixedSizeChunkingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FixedSizeChunkingStrategy();
    }

    @Test
    void testGetName() {
        assertEquals("Fixed-Size Chunking", strategy.getName());
    }

    @Test
    void testChunkWithDefaultSettings() {
        String text = createText(1000);
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
        // Default chunk size is 500, so expect 2+ chunks
        assertTrue(chunks.size() >= 2);
    }

    @Test
    void testChunkWithCustomChunkSize() {
        String text = createText(1000);
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 200);
        config.put("overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 5);
        for (ChunkingStrategy.TextChunk chunk : chunks) {
            assertTrue(chunk.getText().length() <= 200);
        }
    }

    @Test
    void testChunkWithOverlap() {
        String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 10);
        config.put("overlap", 3);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 2);

        // Verify overlap: last 3 chars of chunk[i] should match first 3 chars of chunk[i+1]
        if (chunks.size() >= 2) {
            String chunk1Text = chunks.get(0).getText();
            String chunk2Text = chunks.get(1).getText();

            assertTrue(chunk1Text.length() >= 3);
            assertTrue(chunk2Text.length() >= 3);

            String overlap1 = chunk1Text.substring(chunk1Text.length() - 3);
            String overlap2 = chunk2Text.substring(0, 3);

            assertEquals(overlap1, overlap2);
        }
    }

    @Test
    void testChunkWithEmptyText() {
        ParsedDocument doc = new ParsedDocument("");

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertTrue(chunks.isEmpty());
    }

    @Test
    void testChunkWithNullText() {
        ParsedDocument doc = new ParsedDocument(null);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertTrue(chunks.isEmpty());
    }

    @Test
    void testChunkWithNullConfig() {
        String text = createText(600);
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, null);

        assertFalse(chunks.isEmpty());
        // Should use defaults
    }

    @Test
    void testChunkSizeValidation() {
        ParsedDocument doc = new ParsedDocument("Some text");
        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 0);

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.chunk(doc, config);
        });
    }

    @Test
    void testChunkSizeNegativeValidation() {
        ParsedDocument doc = new ParsedDocument("Some text");
        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", -100);

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.chunk(doc, config);
        });
    }

    @Test
    void testOverlapValidationNegative() {
        ParsedDocument doc = new ParsedDocument("Some text");
        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 100);
        config.put("overlap", -10);

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.chunk(doc, config);
        });
    }

    @Test
    void testOverlapValidationTooLarge() {
        ParsedDocument doc = new ParsedDocument("Some text");
        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 100);
        config.put("overlap", 100);

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.chunk(doc, config);
        });
    }

    @Test
    void testOverlapValidationEqualToChunkSize() {
        ParsedDocument doc = new ParsedDocument("Some text");
        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 100);
        config.put("overlap", 99);

        // Should not throw
        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);
        assertNotNull(chunks);
    }

    @Test
    void testChunkMetadata() {
        String text = createText(600);
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 200);
        config.put("overlap", 50);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        for (int i = 0; i < chunks.size(); i++) {
            ChunkingStrategy.TextChunk chunk = chunks.get(i);
            Map<String, Object> metadata = chunk.getMetadata();

            assertEquals(i, metadata.get("chunk_index"));
            assertEquals("fixed_size", metadata.get("chunk_strategy"));
            assertEquals(200, metadata.get("chunk_size"));
            assertEquals(50, metadata.get("overlap"));
        }
    }

    @Test
    void testChunkOffsets() {
        String text = "0123456789ABCDEFGHIJ";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 10);
        config.put("overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertEquals(2, chunks.size());

        ChunkingStrategy.TextChunk chunk1 = chunks.get(0);
        assertEquals(0, chunk1.getStartOffset());
        assertEquals(10, chunk1.getEndOffset());
        assertEquals("0123456789", chunk1.getText());

        ChunkingStrategy.TextChunk chunk2 = chunks.get(1);
        assertEquals(10, chunk2.getStartOffset());
        assertEquals(20, chunk2.getEndOffset());
        assertEquals("ABCDEFGHIJ", chunk2.getText());
    }

    @Test
    void testChunkOffsetsWithOverlap() {
        String text = "0123456789ABCDEFGHIJ";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 10);
        config.put("overlap", 3);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 2);

        ChunkingStrategy.TextChunk chunk1 = chunks.get(0);
        assertEquals(0, chunk1.getStartOffset());
        assertEquals(10, chunk1.getEndOffset());

        ChunkingStrategy.TextChunk chunk2 = chunks.get(1);
        assertEquals(7, chunk2.getStartOffset()); // 10 - 3 overlap
    }

    @Test
    void testTextSmallerThanChunkSize() {
        String text = "Short";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 100);
        config.put("overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertEquals(1, chunks.size());
        assertEquals("Short", chunks.get(0).getText());
    }

    @Test
    void testTextExactlyChunkSize() {
        String text = createText(100);
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 100);
        config.put("overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertEquals(1, chunks.size());
        assertEquals(100, chunks.get(0).getText().length());
    }

    @Test
    void testLargeDocument() {
        String text = createText(10000);
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 500);
        config.put("overlap", 50);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 20);

        // Verify all chunks have reasonable sizes
        for (ChunkingStrategy.TextChunk chunk : chunks) {
            assertTrue(chunk.getText().length() <= 500);
            assertTrue(chunk.getText().length() > 0);
        }
    }

    @Test
    void testConfigWithIntegerValues() {
        String text = createText(600);
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", Integer.valueOf(200));
        config.put("overlap", Integer.valueOf(20));

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testConfigWithDoubleValues() {
        String text = createText(600);
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 200.5); // Should be converted to int
        config.put("overlap", 20.7);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testConfigWithInvalidTypes() {
        String text = createText(600);
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", "not a number"); // Invalid type
        config.put("overlap", 20);

        // Should fall back to defaults
        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testMinimalChunkSize() {
        String text = "ABCDEFGHIJ";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 1);
        config.put("overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertEquals(10, chunks.size());
        for (ChunkingStrategy.TextChunk chunk : chunks) {
            assertEquals(1, chunk.getText().length());
        }
    }

    @Test
    void testZeroOverlap() {
        String text = createText(1000);
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 100);
        config.put("overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertEquals(10, chunks.size());

        // Verify no overlap
        for (int i = 0; i < chunks.size() - 1; i++) {
            ChunkingStrategy.TextChunk current = chunks.get(i);
            ChunkingStrategy.TextChunk next = chunks.get(i + 1);

            assertEquals(current.getEndOffset(), next.getStartOffset());
        }
    }

    @Test
    void testChunkCoverage() {
        String text = createText(1000);
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 100);
        config.put("overlap", 10);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // Verify first chunk starts at 0
        assertEquals(0, chunks.get(0).getStartOffset());

        // Verify last chunk ends at text length
        ChunkingStrategy.TextChunk lastChunk = chunks.get(chunks.size() - 1);
        assertEquals(text.length(), lastChunk.getEndOffset());
    }

    /**
     * Helper method to create text of specific length.
     */
    private String createText(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('A' + (i % 26)));
        }
        return sb.toString();
    }
}
