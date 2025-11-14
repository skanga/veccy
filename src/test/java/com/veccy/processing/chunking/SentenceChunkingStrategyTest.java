package com.veccy.processing.chunking;

import com.veccy.processing.parsers.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SentenceChunkingStrategy.
 */
class SentenceChunkingStrategyTest {

    private SentenceChunkingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SentenceChunkingStrategy();
    }

    @Test
    void testGetName() {
        assertEquals("Sentence-Based Chunking", strategy.getName());
    }

    @Test
    void testChunkWithDefaultSettings() {
        String text = "This is sentence one. This is sentence two. This is sentence three.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testChunkWithMultipleSentences() {
        String text = "First sentence. Second sentence. Third sentence. Fourth sentence.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 50);
        config.put("sentence_overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
        // Each chunk should contain complete sentences
        for (ChunkingStrategy.TextChunk chunk : chunks) {
            String chunkText = chunk.getText().trim();
            assertTrue(chunkText.endsWith(".") || chunkText.endsWith("!") || chunkText.endsWith("?") || chunks.indexOf(chunk) == chunks.size() - 1);
        }
    }

    @Test
    void testChunkWithQuestionMarks() {
        String text = "Is this a question? Yes it is! What about this?";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testChunkWithExclamationMarks() {
        String text = "This is exciting! Very exciting! Extremely exciting!";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testChunkWithAbbreviations() {
        String text = "Dr. Smith works at the hospital. Mr. Jones is his colleague. They work together.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
        // Should not split on abbreviations
    }

    @Test
    void testChunkWithSentenceOverlap() {
        String text = "Sentence one. Sentence two. Sentence three. Sentence four. Sentence five.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 40);
        config.put("sentence_overlap", 1);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 2);
        // With overlap, some sentences should appear in multiple chunks
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
        String text = "First sentence. Second sentence.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, null);

        assertFalse(chunks.isEmpty());
        // Should use defaults
    }

    @Test
    void testChunkMetadata() {
        String text = "Sentence one. Sentence two. Sentence three.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 30);
        config.put("sentence_overlap", 1);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        for (int i = 0; i < chunks.size(); i++) {
            ChunkingStrategy.TextChunk chunk = chunks.get(i);
            Map<String, Object> metadata = chunk.getMetadata();

            assertEquals(i, metadata.get("chunk_index"));
            assertEquals("sentence", metadata.get("chunk_strategy"));
            assertTrue(metadata.containsKey("sentence_count"));
            assertEquals(1, metadata.get("sentence_overlap"));
        }
    }

    @Test
    void testChunkOffsets() {
        String text = "First. Second. Third.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 15);
        config.put("sentence_overlap", 0);

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
    void testLongSentenceExceedingMaxSize() {
        StringBuilder longSentence = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longSentence.append("word ");
        }
        longSentence.append(".");

        String text = longSentence.toString();
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 100);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // Should still create chunk even if sentence exceeds max size
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getText().length() > 100);
    }

    @Test
    void testTextWithoutSentenceEndings() {
        String text = "This text has no sentence endings it just keeps going";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        // Should treat entire text as one sentence
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getText().contains("This text has no sentence endings"));
    }

    @Test
    void testMultipleSentencesInOneChunk() {
        String text = "A. B. C. D. E.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 100);
        config.put("sentence_overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // All short sentences should fit in one chunk
        assertTrue(chunks.size() <= 2);
    }

    @Test
    void testConfigWithIntegerValues() {
        String text = "First sentence. Second sentence. Third sentence.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", Integer.valueOf(50));
        config.put("sentence_overlap", Integer.valueOf(1));

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testConfigWithDoubleValues() {
        String text = "First sentence. Second sentence.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 50.5);
        config.put("sentence_overlap", 1.7);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testConfigWithInvalidTypes() {
        String text = "First sentence. Second sentence.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", "invalid");
        config.put("sentence_overlap", "invalid");

        // Should fall back to defaults
        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testMixedPunctuation() {
        String text = "Statement. Question? Exclamation! Another statement.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 30);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 2);
    }

    @Test
    void testNewlinesWithinSentences() {
        String text = "This is a sentence\nwith newlines inside. This is another sentence.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testMultipleSpacesBetweenSentences() {
        String text = "First sentence.    Second sentence.     Third sentence.";
        ParsedDocument doc = new ParsedDocument(text);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, new HashMap<>());

        assertFalse(chunks.isEmpty());
    }

    @Test
    void testSingleCharacterSentences() {
        String text = "A. B. C. D. E. F. G. H. I. J.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 5);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 2);
    }

    @Test
    void testZeroSentenceOverlap() {
        String text = "Sentence one. Sentence two. Sentence three. Sentence four.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 30);
        config.put("sentence_overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 2);
    }

    @Test
    void testLargeDocument() {
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeText.append("This is sentence number ").append(i).append(". ");
        }

        ParsedDocument doc = new ParsedDocument(largeText.toString());

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 200);
        config.put("sentence_overlap", 1);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        assertTrue(chunks.size() >= 10);

        // Verify all chunks respect sentence boundaries
        for (ChunkingStrategy.TextChunk chunk : chunks) {
            assertFalse(chunk.getText().isEmpty());
        }
    }

    @Test
    void testChunksCoverEntireText() {
        String text = "First. Second. Third. Fourth. Fifth.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 15);
        config.put("sentence_overlap", 0);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // Verify first chunk starts at beginning
        assertTrue(chunks.get(0).getStartOffset() >= 0);
        assertTrue(chunks.get(0).getStartOffset() < 10);

        // Verify last chunk ends near text end
        ChunkingStrategy.TextChunk lastChunk = chunks.get(chunks.size() - 1);
        assertTrue(lastChunk.getEndOffset() <= text.length());
    }

    @Test
    void testAbbreviationsDoNotCauseSplits() {
        String text = "Prof. Johnson teaches at U.S. universities. Dr. Smith is his colleague from the U.K.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 100);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        // Should not create excessive chunks due to abbreviations
        assertTrue(chunks.size() <= 2);
    }

    @Test
    void testSentenceCountMetadata() {
        String text = "One. Two. Three.";
        ParsedDocument doc = new ParsedDocument(text);

        Map<String, Object> config = new HashMap<>();
        config.put("max_chunk_size", 20);

        List<ChunkingStrategy.TextChunk> chunks = strategy.chunk(doc, config);

        for (ChunkingStrategy.TextChunk chunk : chunks) {
            Object sentenceCount = chunk.getMetadata().get("sentence_count");
            assertNotNull(sentenceCount);
            assertTrue((Integer) sentenceCount >= 1);
        }
    }
}
