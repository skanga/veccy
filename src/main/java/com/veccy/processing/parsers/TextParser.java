package com.veccy.processing.parsers;

import com.veccy.exceptions.ParsingException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for plain text files.
 * <p>
 * Supports .txt and other plain text formats. Extracts text content
 * and basic metadata like file size and line count.
 */
public class TextParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(TextParser.class);

    private static final String[] SUPPORTED_EXTENSIONS = {"txt", "text", "log", "md", "csv"};

    @Override
    public ParsedDocument parse(InputStream input) throws IOException {
        try {
            // Read text content
            String text = IOUtils.toString(input, StandardCharsets.UTF_8);

            // Extract basic metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("parser", "TextParser");
            metadata.put("format", "text/plain");
            metadata.put("character_count", text.length());
            metadata.put("line_count", countLines(text));
            metadata.put("word_count", countWords(text));

            logger.debug("Parsed text document: {} characters, {} lines",
                    text.length(), metadata.get("line_count"));

            return new ParsedDocument(text, metadata);
        } catch (Exception e) {
            throw new ParsingException("Failed to parse text document: " + e.getMessage(), e);
        }
    }

    @Override
    public ParsedDocument parse(Path filePath) throws IOException {
        try {
            if (!Files.exists(filePath)) {
                throw new ParsingException("File not found: " + filePath);
            }

            String text = Files.readString(filePath, StandardCharsets.UTF_8);

            // Extract metadata including file info
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("parser", "TextParser");
            metadata.put("format", "text/plain");
            metadata.put("filename", filePath.getFileName().toString());
            metadata.put("file_size", Files.size(filePath));
            metadata.put("character_count", text.length());
            metadata.put("line_count", countLines(text));
            metadata.put("word_count", countWords(text));

            logger.debug("Parsed text file: {} ({} bytes, {} lines)",
                    filePath.getFileName(), Files.size(filePath), metadata.get("line_count"));

            return new ParsedDocument(text, metadata);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingException("Failed to parse text file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(String fileExtension) {
        if (fileExtension == null) {
            return false;
        }
        String ext = fileExtension.toLowerCase().replaceAll("^\\.", "");
        for (String supported : SUPPORTED_EXTENSIONS) {
            if (supported.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "Text Parser";
    }

    @Override
    public String[] getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS.clone();
    }

    /**
     * Counts the number of lines in the text.
     */
    private int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    /**
     * Counts the number of words in the text.
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        return words.length;
    }
}
