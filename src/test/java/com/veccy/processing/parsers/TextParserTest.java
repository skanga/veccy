package com.veccy.processing.parsers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextParserTest {

    private TextParser parser;

    @BeforeEach
    void setUp() {
        parser = new TextParser();
    }

    @Test
    void testParseSimpleText() throws IOException {
        String content = "Hello, World!\nThis is a test.";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        ParsedDocument result = parser.parse(input);

        assertNotNull(result);
        assertEquals(content, result.getText());
        assertTrue(result.hasText());
        assertEquals(content.length(), result.getTextLength());
    }

    @Test
    void testParseEmptyText() throws IOException {
        String content = "";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        ParsedDocument result = parser.parse(input);

        assertNotNull(result);
        assertEquals("", result.getText());
        assertFalse(result.hasText());
    }

    @Test
    void testParseTextWithMetadata(@TempDir Path tempDir) throws IOException {
        String content = "Line 1\nLine 2\nLine 3";
        Path textFile = tempDir.resolve("test.txt");
        Files.writeString(textFile, content);

        ParsedDocument result = parser.parse(textFile);

        assertNotNull(result);
        assertEquals(content, result.getText());

        Map<String, Object> metadata = result.getMetadata();
        assertEquals("TextParser", metadata.get("parser"));
        assertEquals("test.txt", metadata.get("filename"));
        assertEquals(content.length(), metadata.get("character_count"));
        assertEquals(3, metadata.get("line_count"));
        assertTrue((Integer) metadata.get("word_count") > 0);
    }

    @Test
    void testParseMultilineText() throws IOException {
        String content = "First line\nSecond line\nThird line\nFourth line\n";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        ParsedDocument result = parser.parse(input);

        assertNotNull(result);
        assertEquals(content, result.getText());
    }

    @Test
    void testSupportsExtensions() {
        assertTrue(parser.supports("txt"));
        assertTrue(parser.supports("text"));
        assertTrue(parser.supports("log"));
        assertTrue(parser.supports("md"));
        assertTrue(parser.supports("csv"));
        assertTrue(parser.supports(".txt"));  // With dot
        assertFalse(parser.supports("pdf"));
        assertFalse(parser.supports("docx"));
    }

    @Test
    void testGetSupportedExtensions() {
        String[] extensions = parser.getSupportedExtensions();
        assertNotNull(extensions);
        assertEquals(5, extensions.length);
        assertTrue(containsExtension(extensions, "txt"));
        assertTrue(containsExtension(extensions, "text"));
        assertTrue(containsExtension(extensions, "log"));
        assertTrue(containsExtension(extensions, "md"));
        assertTrue(containsExtension(extensions, "csv"));
    }

    @Test
    void testGetName() {
        assertEquals("Text Parser", parser.getName());
    }

    @Test
    void testParseUTF8Content() throws IOException {
        String content = "Hello 世界! Привет мир! مرحبا العالم";
        InputStream input = new ByteArrayInputStream(content.getBytes());

        ParsedDocument result = parser.parse(input);

        assertNotNull(result);
        assertEquals(content, result.getText());
    }

    @Test
    void testParseLargeText(@TempDir Path tempDir) throws IOException {
        // Create a large text file
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("This is line number ").append(i).append("\n");
        }
        String content = sb.toString();

        Path textFile = tempDir.resolve("large.txt");
        Files.writeString(textFile, content);

        ParsedDocument result = parser.parse(textFile);

        assertNotNull(result);
        assertEquals(content, result.getText());
        Map<String, Object> metadata = result.getMetadata();
        // Note: line count includes final newline, so 1000 lines + 1 trailing = 1001
        assertTrue((Integer) metadata.get("line_count") >= 1000);
    }

    private boolean containsExtension(String[] extensions, String target) {
        for (String ext : extensions) {
            if (ext.equals(target)) {
                return true;
            }
        }
        return false;
    }
}
