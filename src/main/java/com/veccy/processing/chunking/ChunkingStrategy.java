package com.veccy.processing.chunking;

import com.veccy.processing.parsers.ParsedDocument;

import java.util.List;
import java.util.Map;

/**
 * Interface for different document chunking strategies.
 * <p>
 * Chunking is the process of breaking down a document into smaller pieces
 * that can be embedded and searched independently. Different strategies
 * offer different trade-offs:
 * <p>
 * - Fixed-size: Simple, predictable size, may split sentences
 * - Sentence-based: Preserves sentence boundaries, variable size
 * - Paragraph-based: Preserves semantic units, larger chunks
 */
public interface ChunkingStrategy {

    /**
     * Chunks a parsed document into text segments.
     *
     * @param document The parsed document to chunk
     * @param config   Configuration parameters for chunking
     * @return List of text chunks with their metadata
     */
    List<TextChunk> chunk(ParsedDocument document, Map<String, Object> config);

    /**
     * Gets the name of this chunking strategy.
     *
     * @return Strategy name
     */
    String getName();

    /**
     * Represents a text chunk with position information.
     */
    class TextChunk {
        private final String text;
        private final int startOffset;
        private final int endOffset;
        private final Map<String, Object> metadata;

        public TextChunk(String text, int startOffset, int endOffset, Map<String, Object> metadata) {
            this.text = text;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.metadata = metadata;
        }

        public String getText() {
            return text;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public int getLength() {
            return text.length();
        }
    }
}
