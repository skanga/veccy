package com.veccy.exceptions;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ParsingException.
 */
class ParsingExceptionTest {

    @Test
    void testDefaultConstructor() {
        ParsingException exception = new ParsingException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void testMessageConstructor() {
        String message = "Failed to parse document";
        ParsingException exception = new ParsingException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "XML parsing failed";
        IOException cause = new IOException("Unexpected end of file");
        ParsingException exception = new ParsingException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCauseConstructor() {
        IllegalArgumentException cause = new IllegalArgumentException("Invalid character encoding");
        ParsingException exception = new ParsingException(cause);

        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritance() {
        ParsingException exception = new ParsingException("test");

        assertInstanceOf(VeccyException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(ParsingException.class, () -> {
            throw new ParsingException("Parsing error");
        });
    }

    @Test
    void testTypicalParsingErrors() {
        // Invalid format
        ParsingException formatError = new ParsingException("Invalid document format");
        assertTrue(formatError.getMessage().contains("Invalid"));

        // Malformed data
        ParsingException malformedError = new ParsingException("Malformed JSON in document");
        assertTrue(malformedError.getMessage().contains("Malformed"));

        // Encoding error
        ParsingException encodingError = new ParsingException("Unsupported character encoding: UTF-16");
        assertTrue(encodingError.getMessage().contains("encoding"));

        // Syntax error
        ParsingException syntaxError = new ParsingException("Syntax error at line 42");
        assertTrue(syntaxError.getMessage().contains("Syntax"));

        // Unexpected token
        ParsingException tokenError = new ParsingException("Unexpected token: ']'");
        assertTrue(tokenError.getMessage().contains("Unexpected"));
    }

    @Test
    void testDocumentTypeParsingErrors() {
        // PDF parsing
        ParsingException pdfError = new ParsingException("Failed to parse PDF: corrupted header");
        assertTrue(pdfError.getMessage().contains("PDF"));

        // HTML parsing
        ParsingException htmlError = new ParsingException("HTML parsing error: unclosed tag <div>");
        assertTrue(htmlError.getMessage().contains("HTML"));

        // Word document
        ParsingException docxError = new ParsingException("Cannot parse DOCX: invalid file structure");
        assertTrue(docxError.getMessage().contains("DOCX"));

        // Text file
        ParsingException textError = new ParsingException("Text file parsing failed: binary data detected");
        assertTrue(textError.getMessage().contains("Text"));

        // Markdown
        ParsingException mdError = new ParsingException("Markdown parsing error: invalid code block");
        assertTrue(mdError.getMessage().contains("Markdown"));
    }

    @Test
    void testParsingErrorWithLocation() {
        String message = "Parsing error at line 42, column 15: unexpected character '}'";
        ParsingException exception = new ParsingException(message);

        assertTrue(exception.getMessage().contains("line 42"));
        assertTrue(exception.getMessage().contains("column 15"));
    }

    @Test
    void testParsingErrorWithContext() {
        String message = "Failed to parse JSON:\n  \"name\": \"test\",\n  \"value\": [1, 2, }\n           ^--- unexpected token";
        ParsingException exception = new ParsingException(message);

        assertTrue(exception.getMessage().contains("unexpected token"));
    }

    @Test
    void testStructuredDataParsingErrors() {
        // JSON
        ParsingException jsonError = new ParsingException("JSON parsing failed: expected ',' or '}' at position 156");
        assertTrue(jsonError.getMessage().contains("JSON"));

        // XML
        ParsingException xmlError = new ParsingException("XML parsing failed: attribute value not properly quoted");
        assertTrue(xmlError.getMessage().contains("XML"));

        // CSV
        ParsingException csvError = new ParsingException("CSV parsing failed: inconsistent column count");
        assertTrue(csvError.getMessage().contains("CSV"));

        // YAML
        ParsingException yamlError = new ParsingException("YAML parsing failed: invalid indentation");
        assertTrue(yamlError.getMessage().contains("YAML"));
    }
}
