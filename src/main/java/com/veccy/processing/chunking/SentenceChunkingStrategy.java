package com.veccy.processing.chunking;

import com.veccy.processing.parsers.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sentence-based chunking strategy that preserves sentence boundaries.
 * <p>
 * Features:
 * - Preserves complete sentences (no mid-sentence splits)
 * - Groups sentences up to target chunk size
 * - Optional sentence overlap between chunks
 * <p>
 * Benefits:
 * - Maintains semantic coherence
 * - Better for question-answering and retrieval
 * <p>
 * Configuration:
 * - max_chunk_size: Maximum size of each chunk in characters (default 1000)
 * - sentence_overlap: Number of sentences to overlap between chunks (default 1)
 */
public class SentenceChunkingStrategy implements ChunkingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SentenceChunkingStrategy.class);

    private static final int DEFAULT_MAX_CHUNK_SIZE = 1000;
    private static final int DEFAULT_SENTENCE_OVERLAP = 1;

    // Regex pattern to split on sentence boundaries
    // Matches: . ! ? followed by whitespace or end of string
    // Handles common abbreviations (e.g., Dr. Mr. etc.)
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
            "(?<=[.!?])(?<!\\b(?:Dr|Mr|Mrs|Ms|Prof|Sr|Jr|vs|etc|e\\.g|i\\.e)[.])\\s+"
    );

    @Override
    public List<TextChunk> chunk(ParsedDocument document, Map<String, Object> config) {
        String text = document.getText();
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        int maxChunkSize = getConfigInt(config, "max_chunk_size", DEFAULT_MAX_CHUNK_SIZE);
        int sentenceOverlap = getConfigInt(config, "sentence_overlap", DEFAULT_SENTENCE_OVERLAP);

        // Split into sentences
        List<Sentence> sentences = splitIntoSentences(text);

        if (sentences.isEmpty()) {
            return new ArrayList<>();
        }

        // Group sentences into chunks
        List<TextChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        int i = 0;

        while (i < sentences.size()) {
            StringBuilder chunkBuilder = new StringBuilder();
            int chunkStart = sentences.get(i).startOffset;
            int chunkEnd = chunkStart;
            int sentenceCount = 0;

            // Add sentences until we exceed max chunk size
            while (i < sentences.size()) {
                Sentence sentence = sentences.get(i);
                int potentialLength = chunkBuilder.length() + sentence.text.length();

                // If adding this sentence would exceed max size and we already have content, stop
                if (potentialLength > maxChunkSize && chunkBuilder.length() > 0) {
                    break;
                }

                chunkBuilder.append(sentence.text);
                chunkEnd = sentence.endOffset;
                sentenceCount++;
                i++;

                // If we've added at least one sentence and next would exceed, stop
                if (i < sentences.size() && potentialLength > maxChunkSize * 0.8) {
                    break;
                }
            }

            // Create chunk
            Map<String, Object> chunkMetadata = new HashMap<>();
            chunkMetadata.put("chunk_index", chunkIndex);
            chunkMetadata.put("chunk_strategy", "sentence");
            chunkMetadata.put("sentence_count", sentenceCount);
            chunkMetadata.put("sentence_overlap", sentenceOverlap);

            chunks.add(new TextChunk(chunkBuilder.toString(), chunkStart, chunkEnd, chunkMetadata));
            chunkIndex++;

            // Move back for overlap
            if (sentenceOverlap > 0 && i < sentences.size()) {
                i = Math.max(i - sentenceOverlap, i - sentenceCount + 1);
            }
        }

        logger.debug("Created {} sentence-based chunks (avg {} sentences/chunk)",
                chunks.size(), sentences.size() / Math.max(1, chunks.size()));
        return chunks;
    }

    @Override
    public String getName() {
        return "Sentence-Based Chunking";
    }

    /**
     * Splits text into sentences with position tracking.
     */
    private List<Sentence> splitIntoSentences(String text) {
        List<Sentence> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            int end = matcher.start();
            if (end > lastEnd) {
                String sentenceText = text.substring(lastEnd, end).trim();
                if (!sentenceText.isEmpty()) {
                    sentences.add(new Sentence(sentenceText, lastEnd, end));
                }
            }
            lastEnd = matcher.end();
        }

        // Add remaining text as final sentence
        if (lastEnd < text.length()) {
            String sentenceText = text.substring(lastEnd).trim();
            if (!sentenceText.isEmpty()) {
                sentences.add(new Sentence(sentenceText, lastEnd, text.length()));
            }
        }

        // If no sentences found (no punctuation), treat entire text as one sentence
        if (sentences.isEmpty() && !text.trim().isEmpty()) {
            sentences.add(new Sentence(text.trim(), 0, text.length()));
        }

        return sentences;
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
     * Internal class to track sentence text and position.
     */
    private static class Sentence {
        final String text;
        final int startOffset;
        final int endOffset;

        Sentence(String text, int startOffset, int endOffset) {
            this.text = text;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }
}
