package com.veccy.processing.parsers;

import com.veccy.exceptions.ParsingException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for Microsoft Office documents using Apache POI.
 * <p>
 * Supports modern Office formats (Office 2007+):
 * - Word: .docx
 * - Excel: .xlsx
 * - PowerPoint: .pptx
 */
public class OfficeParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(OfficeParser.class);

    private static final String[] SUPPORTED_EXTENSIONS = {
            "docx",   // Word
            "xlsx",   // Excel
            "pptx"    // PowerPoint
    };

    @Override
    public ParsedDocument parse(InputStream input) throws IOException {
        throw new ParsingException("OfficeParser requires file path for format detection. Use parse(Path) instead.");
    }

    @Override
    public ParsedDocument parse(Path filePath) throws IOException {
        try {
            if (!Files.exists(filePath)) {
                throw new ParsingException("File not found: " + filePath);
            }

            String fileName = filePath.getFileName().toString().toLowerCase();
            String text;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("parser", "OfficeParser");
            metadata.put("filename", filePath.getFileName().toString());
            metadata.put("file_size", Files.size(filePath));

            // Determine format and parse accordingly
            if (fileName.endsWith(".docx")) {
                text = parseDocx(filePath, metadata);
            } else if (fileName.endsWith(".xlsx")) {
                text = parseXlsx(filePath, metadata);
            } else if (fileName.endsWith(".pptx")) {
                text = parsePptx(filePath, metadata);
            } else {
                throw new ParsingException("Unsupported file format: " + fileName);
            }

            metadata.put("character_count", text.length());
            metadata.put("word_count", countWords(text));

            logger.debug("Parsed Office document: {} ({} bytes, {} characters)",
                    filePath.getFileName(), Files.size(filePath), text.length());

            return new ParsedDocument(text, metadata);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingException("Failed to parse Office document: " + e.getMessage(), e);
        }
    }

    /**
     * Parses .docx (Word 2007+) files.
     */
    private String parseDocx(Path filePath, Map<String, Object> metadata) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {

            metadata.put("format", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            metadata.put("type", "Word Document (DOCX)");

            // Extract core properties if available
            if (doc.getProperties() != null && doc.getProperties().getCoreProperties() != null) {
                var props = doc.getProperties().getCoreProperties();
                if (props.getTitle() != null) {
                    metadata.put("title", props.getTitle());
                }
                if (props.getCreator() != null) {
                    metadata.put("author", props.getCreator());
                }
                if (props.getSubject() != null) {
                    metadata.put("subject", props.getSubject());
                }
            }

            return extractor.getText();
        }
    }

    /**
     * Parses .xlsx (Excel 2007+) files.
     */
    private String parseXlsx(Path filePath, Map<String, Object> metadata) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            metadata.put("format", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            metadata.put("type", "Excel Spreadsheet (XLSX)");
            metadata.put("sheet_count", workbook.getNumberOfSheets());

            return extractExcelText(workbook);
        }
    }

    /**
     * Extracts text from Excel workbook.
     */
    private String extractExcelText(Workbook workbook) {
        StringBuilder text = new StringBuilder();
        DataFormatter formatter = new DataFormatter();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            text.append("Sheet: ").append(sheet.getSheetName()).append("\n");

            for (Row row : sheet) {
                for (Cell cell : row) {
                    String cellValue = formatter.formatCellValue(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        text.append(cellValue).append("\t");
                    }
                }
                text.append("\n");
            }
            text.append("\n");
        }

        return text.toString();
    }

    /**
     * Parses .pptx (PowerPoint 2007+) files.
     */
    private String parsePptx(Path filePath, Map<String, Object> metadata) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
             XMLSlideShow ppt = new XMLSlideShow(is)) {

            metadata.put("format", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            metadata.put("type", "PowerPoint Presentation (PPTX)");
            metadata.put("slide_count", ppt.getSlides().size());

            StringBuilder text = new StringBuilder();
            int slideNum = 1;

            for (XSLFSlide slide : ppt.getSlides()) {
                text.append("Slide ").append(slideNum++).append(":\n");
                for (XSLFTextShape shape : slide.getPlaceholders()) {
                    String shapeText = shape.getText();
                    if (shapeText != null && !shapeText.trim().isEmpty()) {
                        text.append(shapeText).append("\n");
                    }
                }
                text.append("\n");
            }

            return text.toString();
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
        return "Office Parser";
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
