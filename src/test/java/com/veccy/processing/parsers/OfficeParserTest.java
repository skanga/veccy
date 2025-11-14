package com.veccy.processing.parsers;

import com.veccy.util.OfficeTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OfficeParserTest {

    @TempDir
    Path tempDir;

    @Test
    void testParseDocx() throws Exception {
        Path docxPath = tempDir.resolve("test.docx");
        OfficeTestUtil.createDocx(docxPath.toString(), "This is a test.", "This is another line.");

        OfficeParser parser = new OfficeParser();
        ParsedDocument doc = parser.parse(docxPath);

        assertNotNull(doc);
        String text = doc.getText();
        assertTrue(text.contains("This is a test."));
        assertTrue(text.contains("This is another line."));
    }
}
