package com.veccy.processing.parsers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a parsed document with extracted text and metadata.
 * <p>
 * This class encapsulates the result of document parsing, containing
 * the full text content and optional metadata such as title, author,
 * page count, creation date, etc.
 */
public class ParsedDocument {

    private final String text;
    private final Map<String, Object> metadata;

    /**
     * Creates a parsed document with text and metadata.
     *
     * @param text     The extracted text content
     * @param metadata Optional metadata (title, author, page count, etc.)
     */
    public ParsedDocument(String text, Map<String, Object> metadata) {
        this.text = text != null ? text : "";
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    /**
     * Creates a parsed document with only text content.
     *
     * @param text The extracted text content
     */
    public ParsedDocument(String text) {
        this(text, new HashMap<>());
    }

    /**
     * Gets the extracted text content.
     *
     * @return The text content
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the document metadata.
     *
     * @return A copy of the metadata map
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    /**
     * Gets a specific metadata value.
     *
     * @param key The metadata key
     * @return The metadata value, or null if not present
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Checks if the document has any text content.
     *
     * @return true if text is not empty
     */
    public boolean hasText() {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Gets the character count of the text.
     *
     * @return The number of characters
     */
    public int getTextLength() {
        return text != null ? text.length() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedDocument that = (ParsedDocument) o;
        return Objects.equals(text, that.text) &&
                Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, metadata);
    }

    @Override
    public String toString() {
        return "ParsedDocument{" +
                "textLength=" + getTextLength() +
                ", metadata=" + metadata.keySet() +
                '}';
    }
}
