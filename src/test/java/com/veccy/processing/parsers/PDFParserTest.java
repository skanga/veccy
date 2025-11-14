package com.veccy.processing.parsers;

import com.veccy.util.PDFTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PDFParserTest {

    @TempDir
    Path tempDir;

    @Test
    void testParsePDF() throws Exception {
        Path pdfPath = tempDir.resolve("test.pdf");
        PDFTestUtil.createPdf(pdfPath.toString(), "This is a test.", "This is another line.");

        PDFParser parser = new PDFParser();
        ParsedDocument doc = parser.parse(pdfPath);

        assertNotNull(doc);
        String text = doc.getText();
        assertTrue(text.contains("This is a test."));
        assertTrue(text.contains("This is another line."));
    }
}
