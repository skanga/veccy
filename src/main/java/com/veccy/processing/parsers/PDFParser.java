package com.veccy.processing.parsers;

import com.veccy.exceptions.ParsingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for PDF documents using Apache PDFBox.
 * <p>
 * Extracts text content and metadata including title, author,
 * creation date, page count, and PDF version.
 */
public class PDFParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(PDFParser.class);

    private static final String[] SUPPORTED_EXTENSIONS = {"pdf"};

    @Override
    public ParsedDocument parse(InputStream input) throws IOException {
        try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
            return extractContent(document, null);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingException("Failed to parse PDF document: " + e.getMessage(), e);
        }
    }

    @Override
    public ParsedDocument parse(Path filePath) throws IOException {
        try {
            if (!Files.exists(filePath)) {
                throw new ParsingException("File not found: " + filePath);
            }

            try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
                return extractContent(document, filePath);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingException("Failed to parse PDF file: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text and metadata from a PDDocument.
     */
    private ParsedDocument extractContent(PDDocument document, Path filePath) throws IOException {
        // Extract text content
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);

        // Extract metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parser", "PDFParser");
        metadata.put("format", "application/pdf");
        metadata.put("page_count", document.getNumberOfPages());

        // Add file info if available
        if (filePath != null) {
            metadata.put("filename", filePath.getFileName().toString());
            metadata.put("file_size", Files.size(filePath));
        }

        // Extract PDF metadata
        PDDocumentInformation info = document.getDocumentInformation();
        if (info != null) {
            if (info.getTitle() != null && !info.getTitle().isEmpty()) {
                metadata.put("title", info.getTitle());
            }
            if (info.getAuthor() != null && !info.getAuthor().isEmpty()) {
                metadata.put("author", info.getAuthor());
            }
            if (info.getSubject() != null && !info.getSubject().isEmpty()) {
                metadata.put("subject", info.getSubject());
            }
            if (info.getKeywords() != null && !info.getKeywords().isEmpty()) {
                metadata.put("keywords", info.getKeywords());
            }
            if (info.getCreator() != null && !info.getCreator().isEmpty()) {
                metadata.put("creator", info.getCreator());
            }
            if (info.getProducer() != null && !info.getProducer().isEmpty()) {
                metadata.put("producer", info.getProducer());
            }

            // Creation and modification dates
            Calendar creationDate = info.getCreationDate();
            if (creationDate != null) {
                metadata.put("creation_date", creationDate.getTime().toString());
            }
            Calendar modDate = info.getModificationDate();
            if (modDate != null) {
                metadata.put("modification_date", modDate.getTime().toString());
            }
        }

        // Add text statistics
        metadata.put("character_count", text.length());
        metadata.put("word_count", countWords(text));

        // PDF version
        float version = document.getVersion();
        metadata.put("pdf_version", String.format("%.1f", version));

        logger.debug("Parsed PDF: {} pages, {} characters",
                document.getNumberOfPages(), text.length());

        return new ParsedDocument(text, metadata);
    }

    @Override
    public boolean supports(String fileExtension) {
        if (fileExtension == null) {
            return false;
        }
        String ext = fileExtension.toLowerCase().replaceAll("^\\.", "");
        return "pdf".equals(ext);
    }

    @Override
    public String getName() {
        return "PDF Parser";
    }

    @Override
    public String[] getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS.clone();
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
