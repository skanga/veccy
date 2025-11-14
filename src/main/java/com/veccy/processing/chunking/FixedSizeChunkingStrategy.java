package com.veccy.processing.chunking;

import com.veccy.processing.parsers.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed-size chunking strategy that splits text into chunks of specified character length.
 * <p>
 * Features:
 * - Predictable chunk sizes
 * - Configurable overlap between chunks
 * - Simple and fast
 * <p>
 * Limitations:
 * - May split in the middle of words or sentences
 * - No semantic awareness
 * <p>
 * Configuration:
 * - chunk_size: Target size of each chunk in characters (default 500)
 * - overlap: Number of overlapping characters between chunks (default 50)
 */
public class FixedSizeChunkingStrategy implements ChunkingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FixedSizeChunkingStrategy.class);

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 50;

    @Override
    public List<TextChunk> chunk(ParsedDocument document, Map<String, Object> config) {
        String text = document.getText();
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        int chunkSize = getConfigInt(config, "chunk_size", DEFAULT_CHUNK_SIZE);
        int overlap = getConfigInt(config, "overlap", DEFAULT_OVERLAP);

        // Validate configuration
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunk_size must be positive");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap must be non-negative and less than chunk_size");
        }

        List<TextChunk> chunks = new ArrayList<>();
        int textLength = text.length();
        int position = 0;
        int chunkIndex = 0;

        while (position < textLength) {
            int endPosition = Math.min(position + chunkSize, textLength);
            String chunkText = text.substring(position, endPosition);

            Map<String, Object> chunkMetadata = new HashMap<>();
            chunkMetadata.put("chunk_index", chunkIndex);
            chunkMetadata.put("chunk_strategy", "fixed_size");
            chunkMetadata.put("chunk_size", chunkSize);
            chunkMetadata.put("overlap", overlap);

            chunks.add(new TextChunk(chunkText, position, endPosition, chunkMetadata));

            // Move forward by (chunkSize - overlap)
            position += chunkSize - overlap;
            chunkIndex++;

            // Avoid infinite loop if overlap >= chunkSize
            if (overlap >= chunkSize - 1 && position <= endPosition - chunkSize) {
                position = endPosition;
            }
        }

        logger.debug("Created {} fixed-size chunks (size={}, overlap={})", chunks.size(), chunkSize, overlap);
        return chunks;
    }

    @Override
    public String getName() {
        return "Fixed-Size Chunking";
    }

    private int getConfigInt(Map<String, Object> config, String key, int defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
