package com.veccy.processing.chunking;

import com.veccy.processing.parsers.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paragraph-based chunking strategy that preserves paragraph boundaries.
 * <p>
 * Features:
 * - Preserves complete paragraphs (no mid-paragraph splits)
 * - Groups paragraphs up to target chunk size
 * - Optional paragraph overlap between chunks
 * <p>
 * Benefits:
 * - Maintains document structure
 * - Preserves semantic context within sections
 * - Good for documents with clear paragraph structure
 * <p>
 * Limitations:
 * - Variable chunk sizes
 * - Large paragraphs may exceed target size
 * <p>
 * Configuration:
 * - max_chunk_size: Maximum size of each chunk in characters (default 2000)
 * - paragraph_overlap: Number of paragraphs to overlap between chunks (default 0)
 * - min_paragraph_length: Minimum length to consider as paragraph (default 10)
 */
public class ParagraphChunkingStrategy implements ChunkingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ParagraphChunkingStrategy.class);

    private static final int DEFAULT_MAX_CHUNK_SIZE = 2000;
    private static final int DEFAULT_PARAGRAPH_OVERLAP = 0;
    private static final int DEFAULT_MIN_PARAGRAPH_LENGTH = 10;

    @Override
    public List<TextChunk> chunk(ParsedDocument document, Map<String, Object> config) {
        String text = document.getText();
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        int maxChunkSize = getConfigInt(config, "max_chunk_size", DEFAULT_MAX_CHUNK_SIZE);
        int paragraphOverlap = getConfigInt(config, "paragraph_overlap", DEFAULT_PARAGRAPH_OVERLAP);
        int minParagraphLength = getConfigInt(config, "min_paragraph_length", DEFAULT_MIN_PARAGRAPH_LENGTH);

        // Split into paragraphs
        List<Paragraph> paragraphs = splitIntoParagraphs(text, minParagraphLength);

        if (paragraphs.isEmpty()) {
            return new ArrayList<>();
        }

        // Group paragraphs into chunks
        List<TextChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        int i = 0;

        while (i < paragraphs.size()) {
            StringBuilder chunkBuilder = new StringBuilder();
            int chunkStart = paragraphs.get(i).startOffset;
            int chunkEnd = chunkStart;
            int paragraphCount = 0;

            // Add paragraphs until we exceed max chunk size
            while (i < paragraphs.size()) {
                Paragraph paragraph = paragraphs.get(i);

                // If this single paragraph is larger than max size, include it anyway
                if (chunkBuilder.length() == 0 && paragraph.text.length() > maxChunkSize) {
                    chunkBuilder.append(paragraph.text);
                    chunkEnd = paragraph.endOffset;
                    paragraphCount++;
                    i++;
                    break;
                }

                int potentialLength = chunkBuilder.length() + paragraph.text.length();

                // If adding this paragraph would exceed max size and we already have content, stop
                if (potentialLength > maxChunkSize && chunkBuilder.length() > 0) {
                    break;
                }

                if (chunkBuilder.length() > 0) {
                    chunkBuilder.append("\n\n");
                }
                chunkBuilder.append(paragraph.text);
                chunkEnd = paragraph.endOffset;
                paragraphCount++;
                i++;
            }

            // Create chunk
            Map<String, Object> chunkMetadata = new HashMap<>();
            chunkMetadata.put("chunk_index", chunkIndex);
            chunkMetadata.put("chunk_strategy", "paragraph");
            chunkMetadata.put("paragraph_count", paragraphCount);
            chunkMetadata.put("paragraph_overlap", paragraphOverlap);

            chunks.add(new TextChunk(chunkBuilder.toString(), chunkStart, chunkEnd, chunkMetadata));
            chunkIndex++;

            // Move back for overlap
            if (paragraphOverlap > 0 && i < paragraphs.size()) {
                i = Math.max(i - paragraphOverlap, i - paragraphCount + 1);
            }
        }

        logger.debug("Created {} paragraph-based chunks (avg {} paragraphs/chunk)",
                chunks.size(), paragraphs.size() / Math.max(1, chunks.size()));
        return chunks;
    }

    @Override
    public String getName() {
        return "Paragraph-Based Chunking";
    }

    /**
     * Splits text into paragraphs.
     * <p>
     * Paragraphs are identified by:
     * - Double newlines (\n\n or \r\n\r\n)
     * - Single newlines followed by significant indentation
     */
    private List<Paragraph> splitIntoParagraphs(String text, int minLength) {
        List<Paragraph> paragraphs = new ArrayList<>();

        // Split on double newlines (or more)
        String[] rawParagraphs = text.split("\\n\\s*\\n+");

        int currentOffset = 0;
        for (String rawPara : rawParagraphs) {
            String paraText = rawPara.trim();

            // Skip empty or too-short paragraphs
            if (paraText.length() >= minLength) {
                // Find actual position in original text
                int startOffset = text.indexOf(paraText, currentOffset);
                if (startOffset >= 0) {
                    int endOffset = startOffset + paraText.length();
                    paragraphs.add(new Paragraph(paraText, startOffset, endOffset));
                    currentOffset = endOffset;
                }
            } else if (!paraText.isEmpty()) {
                // Move offset forward even for skipped paragraphs
                int skipOffset = text.indexOf(paraText, currentOffset);
                if (skipOffset >= 0) {
                    currentOffset = skipOffset + paraText.length();
                }
            }
        }

        // If no paragraphs found, treat entire text as one paragraph
        if (paragraphs.isEmpty() && text.trim().length() >= minLength) {
            paragraphs.add(new Paragraph(text.trim(), 0, text.length()));
        }

        return paragraphs;
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

    /**
     * Internal class to track paragraph text and position.
     */
    private static class Paragraph {
        final String text;
        final int startOffset;
        final int endOffset;

        Paragraph(String text, int startOffset, int endOffset) {
            this.text = text;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }
}
