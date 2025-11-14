package com.veccy.processing;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ProcessedChunk data class.
 */
class ProcessedChunkTest {

    @Test
    void testConstructorWithAllParameters() {
        String text = "Chunk text content";
        int chunkIndex = 5;
        String documentId = "doc123";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        ProcessedChunk chunk = new ProcessedChunk(text, chunkIndex, documentId, metadata);

        assertEquals(text, chunk.getText());
        assertEquals(chunkIndex, chunk.getChunkIndex());
        assertEquals(documentId, chunk.getDocumentId());
        assertEquals(1, chunk.getMetadata().size());
        assertEquals("value1", chunk.getMetadata().get("key1"));
        assertNull(chunk.getEmbedding());
        assertFalse(chunk.hasEmbedding());
    }

    @Test
    void testConstructorWithNullText() {
        ProcessedChunk chunk = new ProcessedChunk(null, 0, "doc1", null);

        assertEquals("", chunk.getText());
        assertEquals(0, chunk.getTextLength());
    }

    @Test
    void testConstructorWithNullMetadata() {
        ProcessedChunk chunk = new ProcessedChunk("text", 0, "doc1", null);

        assertNotNull(chunk.getMetadata());
        assertTrue(chunk.getMetadata().isEmpty());
    }

    @Test
    void testConstructorMakesDefensiveCopy() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        ProcessedChunk chunk = new ProcessedChunk("text", 0, "doc1", metadata);

        // Modify original metadata
        metadata.put("key2", "value2");

        // Chunk's metadata should not be affected
        assertEquals(1, chunk.getMetadata().size());
        assertFalse(chunk.getMetadata().containsKey("key2"));
    }

    @Test
    void testGetMetadataReturnsDefensiveCopy() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        ProcessedChunk chunk = new ProcessedChunk("text", 0, "doc1", metadata);

        // Get and modify metadata
        Map<String, Object> retrievedMetadata = chunk.getMetadata();
        retrievedMetadata.put("key2", "value2");

        // Original chunk's metadata should not be affected
        assertEquals(1, chunk.getMetadata().size());
        assertFalse(chunk.getMetadata().containsKey("key2"));
    }

    @Test
    void testSetAndGetEmbedding() {
        ProcessedChunk chunk = new ProcessedChunk("text", 0, "doc1", null);

        assertNull(chunk.getEmbedding());
        assertFalse(chunk.hasEmbedding());

        double[] embedding = {0.1, 0.2, 0.3, 0.4};
        chunk.setEmbedding(embedding);

        assertNotNull(chunk.getEmbedding());
        assertTrue(chunk.hasEmbedding());
        assertArrayEquals(embedding, chunk.getEmbedding());
    }

    @Test
    void testSetEmbeddingToNull() {
        ProcessedChunk chunk = new ProcessedChunk("text", 0, "doc1", null);

        double[] embedding = {0.1, 0.2, 0.3};
        chunk.setEmbedding(embedding);
        assertTrue(chunk.hasEmbedding());

        chunk.setEmbedding(null);
        assertFalse(chunk.hasEmbedding());
        assertNull(chunk.getEmbedding());
    }

    @Test
    void testHasEmbeddingWithEmptyArray() {
        ProcessedChunk chunk = new ProcessedChunk("text", 0, "doc1", null);

        chunk.setEmbedding(new double[0]);
        assertFalse(chunk.hasEmbedding());
    }

    @Test
    void testHasEmbeddingWithNonEmptyArray() {
        ProcessedChunk chunk = new ProcessedChunk("text", 0, "doc1", null);

        chunk.setEmbedding(new double[]{1.0});
        assertTrue(chunk.hasEmbedding());
    }

    @Test
    void testGetTextLength() {
        ProcessedChunk chunk1 = new ProcessedChunk("Hello", 0, "doc1", null);
        assertEquals(5, chunk1.getTextLength());

        ProcessedChunk chunk2 = new ProcessedChunk("", 0, "doc1", null);
        assertEquals(0, chunk2.getTextLength());

        ProcessedChunk chunk3 = new ProcessedChunk(null, 0, "doc1", null);
        assertEquals(0, chunk3.getTextLength());
    }

    @Test
    void testGetChunkId() {
        ProcessedChunk chunk = new ProcessedChunk("text", 5, "document_123", null);

        assertEquals("document_123_5", chunk.getChunkId());
    }

    @Test
    void testGetChunkIdWithZeroIndex() {
        ProcessedChunk chunk = new ProcessedChunk("text", 0, "doc_xyz", null);

        assertEquals("doc_xyz_0", chunk.getChunkId());
    }

    @Test
    void testGetChunkIdWithLargeIndex() {
        ProcessedChunk chunk = new ProcessedChunk("text", 999, "doc", null);

        assertEquals("doc_999", chunk.getChunkId());
    }

    @Test
    void testToString() {
        ProcessedChunk chunk = new ProcessedChunk("Sample text", 3, "document_42", null);

        String toString = chunk.toString();

        assertTrue(toString.contains("ProcessedChunk"));
        assertTrue(toString.contains("documentId='document_42'"));
        assertTrue(toString.contains("chunkIndex=3"));
        assertTrue(toString.contains("textLength=11"));
        assertTrue(toString.contains("hasEmbedding=false"));
    }

    @Test
    void testToStringWithEmbedding() {
        ProcessedChunk chunk = new ProcessedChunk("text", 0, "doc1", null);
        chunk.setEmbedding(new double[]{0.1, 0.2});

        String toString = chunk.toString();

        assertTrue(toString.contains("hasEmbedding=true"));
    }

    @Test
    void testMultipleDifferentChunks() {
        ProcessedChunk chunk1 = new ProcessedChunk("text1", 0, "doc1", null);
        ProcessedChunk chunk2 = new ProcessedChunk("text2", 1, "doc1", null);
        ProcessedChunk chunk3 = new ProcessedChunk("text3", 0, "doc2", null);

        assertEquals("doc1_0", chunk1.getChunkId());
        assertEquals("doc1_1", chunk2.getChunkId());
        assertEquals("doc2_0", chunk3.getChunkId());

        assertNotEquals(chunk1.getChunkId(), chunk2.getChunkId());
        assertNotEquals(chunk1.getChunkId(), chunk3.getChunkId());
    }

    @Test
    void testChunkWithComplexMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_file", "/path/to/file.pdf");
        metadata.put("start_offset", 100);
        metadata.put("end_offset", 500);
        metadata.put("page_number", 5);

        ProcessedChunk chunk = new ProcessedChunk("text", 2, "doc1", metadata);

        Map<String, Object> retrievedMetadata = chunk.getMetadata();
        assertEquals(4, retrievedMetadata.size());
        assertEquals("/path/to/file.pdf", retrievedMetadata.get("source_file"));
        assertEquals(100, retrievedMetadata.get("start_offset"));
        assertEquals(500, retrievedMetadata.get("end_offset"));
        assertEquals(5, retrievedMetadata.get("page_number"));
    }

    @Test
    void testChunkWithLongText() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("word ");
        }

        ProcessedChunk chunk = new ProcessedChunk(longText.toString(), 0, "doc1", null);

        assertEquals(5000, chunk.getTextLength());
        assertTrue(chunk.getText().startsWith("word word"));
    }

    @Test
    void testChunkWithHighDimensionalEmbedding() {
        ProcessedChunk chunk = new ProcessedChunk("text", 0, "doc1", null);

        double[] embedding = new double[1536]; // Common embedding size
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = Math.random();
        }

        chunk.setEmbedding(embedding);

        assertTrue(chunk.hasEmbedding());
        assertEquals(1536, chunk.getEmbedding().length);
    }

    @Test
    void testNegativeChunkIndex() {
        ProcessedChunk chunk = new ProcessedChunk("text", -1, "doc1", null);

        assertEquals(-1, chunk.getChunkIndex());
        assertEquals("doc1_-1", chunk.getChunkId());
    }

    @Test
    void testEmptyDocumentId() {
        ProcessedChunk chunk = new ProcessedChunk("text", 0, "", null);

        assertEquals("", chunk.getDocumentId());
        assertEquals("_0", chunk.getChunkId());
    }

    @Test
    void testDocumentIdWithSpecialCharacters() {
        ProcessedChunk chunk = new ProcessedChunk("text", 5, "doc-name_2024.01.15", null);

        assertEquals("doc-name_2024.01.15", chunk.getDocumentId());
        assertEquals("doc-name_2024.01.15_5", chunk.getChunkId());
    }
}
