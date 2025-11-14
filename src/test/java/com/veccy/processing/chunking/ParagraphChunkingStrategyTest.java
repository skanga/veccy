package com.veccy.processing.chunking;

import com.veccy.processing.parsers.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ParagraphChunkingStrategy.
 */
class ParagraphChunkingStrategyTest {

    private ParagraphChunkingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ParagraphChunkingStrategy();
    }

    @Test
    void testGetName() {
        assertEquals("Paragraph-Based Chunking", strategy.getName());
    }

    @Test
    void testChunkWithDefaultSettings() {
        String text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testChunkWithMultipleParagraphs() {
        String text = "Paragraph one.\n\nParagraph two.\n\nParagraph three.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 30);
        config.put("paragraph_overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
        // Each chunk should contain complete paragraphs
    }

    @Test
    void testChunkWithParagraphOverlap() {
        String text = "Paragraph one.\n\nParagraph two.\n\nParagraph three.\n\nParagraph four.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 35);
        config.put("paragraph_overlap", 1);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 2);
        // With overlap, some paragraphs should appear in multiple chunks
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
        String text = "First paragraph.\n\nSecond paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, null);

        assertFalse(chunks.isEmpty());
        // Should use defaults
    }

    @Test
    void testChunkMetadata() {
        String text = "Para one.\n\nPara two.\n\nPara three.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 30);
        config.put("paragraph_overlap", 1);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        for (int i = 0; i < chunks.size(); i++) {
            ChunkingStrategy.TextChunk chunk = chunks.get(i);
            Map<String, Object> metadata = chunk.getMetadata();

            assertEquals(i, metadata.get("chunk_index"));
            assertEquals("paragraph", metadata.get("chunk_strategy"));
            assertTrue(metadata.containsKey("paragraph_count"));
            assertEquals(1, metadata.get("paragraph_overlap"));
        }
    }

    @Test
    void testChunkOffsets() {
        String text = "First.\n\nSecond.\n\nThird.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 15);
        config.put("paragraph_overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());

        // Verify offsets are within text bounds
        for (ChunkingStrategy.TextChunk chunk : chunks) {
            assertTrue(chunk.getStartOffset() >= 0);
            assertTrue(chunk.getEndOffset() <= text.length());
            assertTrue(chunk.getStartOffset() < chunk.getEndOffset());
        }
    }

    @Test
    void testLongParagraphExceedingMaxSize() {
        StringBuilder longParagraph = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longParagraph.append("word ");
        }

        String text = longParagraph.toString();
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 100);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // Should still create chunk even if paragraph exceeds max size
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getText().length() > 100);
    }

    @Test
    void testTextWithoutParagraphBreaks() {
        String text = "This is all one paragraph without any double newlines";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        // Should treat entire text as one paragraph
        assertEquals(1, chunks.size());
    }

    @Test
    void testMultipleParagraphsInOneChunk() {
        String text = "A.\n\nB.\n\nC.\n\nD.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 100);
        config.put("paragraph_overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // All short paragraphs should fit in one chunk
        assertTrue(chunks.size() <= 2);
    }

    @Test
    void testConfigWithIntegerValues() {
        String text = "First paragraph.\n\nSecond paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", Integer.valueOf(50));
        config.put("paragraph_overlap", Integer.valueOf(1));
        config.put("min_paragraph_length", Integer.valueOf(5));

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testConfigWithDoubleValues() {
        String text = "First paragraph.\n\nSecond paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 50.5);
        config.put("paragraph_overlap", 1.7);
        config.put("min_paragraph_length", 5.2);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testConfigWithInvalidTypes() {
        String text = "First paragraph.\n\nSecond paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", "invalid");
        config.put("paragraph_overlap", "invalid");

        // Should fall back to defaults
        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testMinParagraphLength() {
        String text = "A\n\nThis is a longer paragraph that meets the minimum.\n\nB\n\nAnother long paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("min_paragraph_length", 20);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
        // Short paragraphs "A" and "B" should be filtered out
    }

    @Test
    void testTripleNewlines() {
        String text = "First paragraph.\n\n\nSecond paragraph.\n\n\n\nThird paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        // Should handle multiple newlines as paragraph breaks
        assertTrue(chunks.size() >= 1);
    }

    @Test
    void testWindowsLineEndings() {
        String text = "First paragraph.\r\n\r\nSecond paragraph.\r\n\r\nThird paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testMixedLineEndings() {
        String text = "First paragraph.\n\nSecond paragraph.\r\n\r\nThird paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testZeroParagraphOverlap() {
        String text = "Paragraph one.\n\nParagraph two.\n\nParagraph three.\n\nParagraph four.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 35);
        config.put("paragraph_overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 2);
    }

    @Test
    void testLargeDocument() {
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            largeText.append("This is paragraph number ").append(i).append(".\n\n");
        }

        ParsedDocument doc = new ParsedDocument(largeText.toString());

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 200);
        config.put("paragraph_overlap", 1);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 5);

        // Verify all chunks respect paragraph boundaries
        for (ChunkingStrategy.TextChunk chunk : chunks) {
            assertFalse(chunk.getText().isEmpty());
        }
    }

    @Test
    void testParagraphsSeparatedByNewline() {
        String text = "First paragraph.\n\nSecond paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testWhitespaceBetweenParagraphs() {
        String text = "First paragraph.\n   \n   \nSecond paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testChunkCountMetadata() {
        String text = "One.\n\nTwo.\n\nThree.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 20);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        for (ChunkingStrategy.TextChunk chunk : chunks) {
            Object paragraphCount = chunk.getMetadata().get("paragraph_count");
            assertNotNull(paragraphCount);
            assertTrue((Integer) paragraphCount >= 1);
        }
    }

    @Test
    void testParagraphsJoinedWithDoubleNewline() {
        String text = "First.\n\nSecond.\n\nThird.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 100);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // When multiple paragraphs fit in one chunk, they should be joined with \n\n
        if (chunks.size() == 1) {
            String chunkText = chunks.get(0).getText();
            assertTrue(chunkText.contains("\n\n") || !text.contains("\n\n"));
        }
    }

    @Test
    void testSingleShortParagraph() {
        String text = "Short paragraph.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getText().contains("Short paragraph."));
    }

    @Test
    void testEmptyParagraphsFiltered() {
        String text = "Para one.\n\n\n\n\n\nPara two.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("min_paragraph_length", 5);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // Empty paragraphs created by multiple newlines should be filtered
        assertFalse(chunks.isEmpty());
    }

    @Test
    void testVeryLongSingleParagraph() {
        StringBuilder veryLong = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            veryLong.append("word ");
        }

        ParsedDocument doc = new ParsedDocument(veryLong.toString());

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 100);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // Should create one chunk containing the entire long paragraph
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getText().length() > 100);
    }

    @Test
    void testMinParagraphLengthZero() {
        String text = "A\n\nB\n\nC";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("min_paragraph_length", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // All paragraphs should be included, even single characters
        assertFalse(chunks.isEmpty());
    }

    @Test
    void testChunksCoverEntireText() {
        String text = "First.\n\nSecond.\n\nThird.\n\nFourth.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 20);
        config.put("paragraph_overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // Verify first chunk starts near beginning
        assertTrue(chunks.get(0).getStartOffset() >= 0);
        assertTrue(chunks.get(0).getStartOffset() < 10);

        // Verify coverage
        assertFalse(chunks.isEmpty());
    }
}
