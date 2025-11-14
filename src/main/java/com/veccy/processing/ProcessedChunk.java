package com.veccy.processing;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a processed chunk of a document with its metadata.
 * <p>
 * A chunk is a portion of a document that has been:
 * - Extracted from the original document
 * - Assigned position information (chunk index, character offsets)
 * - Tagged with source document metadata
 * - Optionally embedded as a vector
 */
public class ProcessedChunk {

    private final String text;
    private final Map<String, Object> metadata;
    private final int chunkIndex;
    private final String documentId;
    private double[] embedding;

    /**
     * Creates a new ProcessedChunk.
     *
     * @param text       The chunk text content
     * @param chunkIndex The index of this chunk in the document (0-based)
     * @param documentId Identifier for the source document
     * @param metadata   Additional metadata for this chunk
     */
    public ProcessedChunk(String text, int chunkIndex, String documentId, Map<String, Object> metadata) {
        this.text = text != null ? text : "";
        this.chunkIndex = chunkIndex;
        this.documentId = documentId;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.embedding = null;
    }

    public String getText() {
        return text;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getDocumentId() {
        return documentId;
    }

    public double[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
    }

    public boolean hasEmbedding() {
        return embedding != null && embedding.length > 0;
    }

    public int getTextLength() {
        return text.length();
    }

    /**
     * Gets a unique identifier for this chunk.
     *
     * @return Chunk ID in format "documentId_chunkIndex"
     */
    public String getChunkId() {
        return documentId + "_" + chunkIndex;
    }

    @Override
    public String toString() {
        return "ProcessedChunk{" +
                "documentId='" + documentId + '\'' +
                ", chunkIndex=" + chunkIndex +
                ", textLength=" + text.length() +
                ", hasEmbedding=" + hasEmbedding() +
                '}';
    }
}
