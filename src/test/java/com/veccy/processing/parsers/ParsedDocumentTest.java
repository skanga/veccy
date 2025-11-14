package com.veccy.processing.parsers;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ParsedDocument data class.
 */
class ParsedDocumentTest {

    @Test
    void testConstructorWithTextAndMetadata() {
        String text = "This is the document text.";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Test Document");
        metadata.put("author", "John Doe");

        ParsedDocument doc = new ParsedDocument(text, metadata);

        assertEquals(text, doc.getText());
        assertEquals(2, doc.getMetadata().size());
        assertEquals("Test Document", doc.getMetadata().get("title"));
        assertEquals("John Doe", doc.getMetadata().get("author"));
    }

    @Test
    void testConstructorWithTextOnly() {
        String text = "Simple document text.";
        ParsedDocument doc = new ParsedDocument(text);

        assertEquals(text, doc.getText());
        assertTrue(doc.getMetadata().isEmpty());
        assertNotNull(doc.getMetadata());
    }

    @Test
    void testConstructorWithNullText() {
        ParsedDocument doc = new ParsedDocument(null);

        assertEquals("", doc.getText());
        assertTrue(doc.getMetadata().isEmpty());
    }

    @Test
    void testConstructorWithNullMetadata() {
        String text = "Document text";
        ParsedDocument doc = new ParsedDocument(text, null);

        assertEquals(text, doc.getText());
        assertNotNull(doc.getMetadata());
        assertTrue(doc.getMetadata().isEmpty());
    }

    @Test
    void testConstructorMakesDefensiveCopy() {
        String text = "Document text";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        ParsedDocument doc = new ParsedDocument(text, metadata);

        // Modify original metadata
        metadata.put("key2", "value2");

        // Document's metadata should not be affected
        assertEquals(1, doc.getMetadata().size());
        assertFalse(doc.getMetadata().containsKey("key2"));
    }

    @Test
    void testGetMetadataReturnsDefensiveCopy() {
        String text = "Document text";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        ParsedDocument doc = new ParsedDocument(text, metadata);

        // Get and modify metadata
        Map<String, Object> retrievedMetadata = doc.getMetadata();
        retrievedMetadata.put("key2", "value2");

        // Original document's metadata should not be affected
        assertEquals(1, doc.getMetadata().size());
        assertFalse(doc.getMetadata().containsKey("key2"));
    }

    @Test
    void testGetMetadataByKey() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Test Title");
        metadata.put("pages", 42);

        ParsedDocument doc = new ParsedDocument("text", metadata);

        assertEquals("Test Title", doc.getMetadata("title"));
        assertEquals(42, doc.getMetadata("pages"));
        assertNull(doc.getMetadata("nonexistent"));
    }

    @Test
    void testHasTextWithNonEmptyText() {
        ParsedDocument doc = new ParsedDocument("Some content");
        assertTrue(doc.hasText());
    }

    @Test
    void testHasTextWithEmptyString() {
        ParsedDocument doc = new ParsedDocument("");
        assertFalse(doc.hasText());
    }

    @Test
    void testHasTextWithWhitespaceOnly() {
        ParsedDocument doc = new ParsedDocument("   \n\t   ");
        assertFalse(doc.hasText());
    }

    @Test
    void testHasTextWithNull() {
        ParsedDocument doc = new ParsedDocument(null);
        assertFalse(doc.hasText());
    }

    @Test
    void testGetTextLength() {
        ParsedDocument doc1 = new ParsedDocument("Hello");
        assertEquals(5, doc1.getTextLength());

        ParsedDocument doc2 = new ParsedDocument("");
        assertEquals(0, doc2.getTextLength());

        ParsedDocument doc3 = new ParsedDocument(null);
        assertEquals(0, doc3.getTextLength());
    }

    @Test
    void testEqualsWithSameContent() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Title");

        ParsedDocument doc1 = new ParsedDocument("text", metadata);
        ParsedDocument doc2 = new ParsedDocument("text", metadata);

        assertEquals(doc1, doc2);
        assertEquals(doc1.hashCode(), doc2.hashCode());
    }

    @Test
    void testEqualsWithDifferentText() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Title");

        ParsedDocument doc1 = new ParsedDocument("text1", metadata);
        ParsedDocument doc2 = new ParsedDocument("text2", metadata);

        assertNotEquals(doc1, doc2);
    }

    @Test
    void testEqualsWithDifferentMetadata() {
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("title", "Title1");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("title", "Title2");

        ParsedDocument doc1 = new ParsedDocument("text", metadata1);
        ParsedDocument doc2 = new ParsedDocument("text", metadata2);

        assertNotEquals(doc1, doc2);
    }

    @Test
    void testEqualsWithNull() {
        ParsedDocument doc = new ParsedDocument("text");
        assertNotEquals(null, doc);
    }

    @Test
    void testEqualsWithDifferentType() {
        ParsedDocument doc = new ParsedDocument("text");
        assertNotEquals("text", doc);
    }

    @Test
    void testEqualsSameInstance() {
        ParsedDocument doc = new ParsedDocument("text");
        assertEquals(doc, doc);
    }

    @Test
    void testHashCodeConsistency() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Title");

        ParsedDocument doc = new ParsedDocument("text", metadata);

        int hashCode1 = doc.hashCode();
        int hashCode2 = doc.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    void testToString() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Title");
        metadata.put("author", "Author");

        ParsedDocument doc = new ParsedDocument("Hello World", metadata);

        String toString = doc.toString();

        assertTrue(toString.contains("ParsedDocument"));
        assertTrue(toString.contains("textLength=11"));
        assertTrue(toString.contains("metadata="));
    }

    @Test
    void testToStringWithEmptyMetadata() {
        ParsedDocument doc = new ParsedDocument("text");
        String toString = doc.toString();

        assertTrue(toString.contains("ParsedDocument"));
        assertTrue(toString.contains("metadata=[]"));
    }

    @Test
    void testComplexMetadataTypes() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("string", "value");
        metadata.put("integer", 42);
        metadata.put("double", 3.14);
        metadata.put("boolean", true);
        metadata.put("null", null);

        ParsedDocument doc = new ParsedDocument("text", metadata);

        assertEquals("value", doc.getMetadata("string"));
        assertEquals(42, doc.getMetadata("integer"));
        assertEquals(3.14, doc.getMetadata("double"));
        assertEquals(true, doc.getMetadata("boolean"));
        assertNull(doc.getMetadata("null"));
    }

    @Test
    void testLargeText() {
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeText.append("text ");
        }

        ParsedDocument doc = new ParsedDocument(largeText.toString());

        assertTrue(doc.hasText());
        assertEquals(50000, doc.getTextLength());
    }
}
