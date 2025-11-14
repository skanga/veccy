package com.veccy.processing.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Interface for document parsers that extract text and metadata from various file formats.
 * <p>
 * Implementations should handle specific file types (PDF, DOCX, HTML, etc.) and
 * extract both text content and relevant metadata.
 */
public interface DocumentParser {

    /**
     * Parses a document from an input stream.
     *
     * @param input The input stream containing the document
     * @return Parsed document with text and metadata
     * @throws IOException If an I/O error occurs during parsing
     */
    ParsedDocument parse(InputStream input) throws IOException;

    /**
     * Parses a document from a file path.
     *
     * @param filePath The path to the document file
     * @return Parsed document with text and metadata
     * @throws IOException If an I/O error occurs during parsing
     */
    ParsedDocument parse(Path filePath) throws IOException;

    /**
     * Checks if this parser supports a given file extension.
     *
     * @param fileExtension The file extension (e.g., "pdf", "docx")
     * @return true if this parser can handle the file type
     */
    boolean supports(String fileExtension);

    /**
     * Gets the name of this parser.
     *
     * @return The parser name (e.g., "PDF Parser", "Text Parser")
     */
    String getName();

    /**
     * Gets the supported file extensions.
     *
     * @return Array of supported extensions (e.g., ["pdf"], ["doc", "docx"])
     */
    String[] getSupportedExtensions();
}
