package com.veccy.processing.parsers;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class HTMLParserTest {

    @Test
    void testParseHTML() throws Exception {
        HTMLParser parser = new HTMLParser();
        URL resource = getClass().getClassLoader().getResource("test.html");
        assertNotNull(resource, "Test HTML file not found");
        Path path = new File(resource.toURI()).toPath();

        ParsedDocument doc = parser.parse(path);

        assertNotNull(doc);
        String text = doc.getText();
        assertTrue(text.contains("This is a heading"));
        assertTrue(text.contains("This is a paragraph."));
        assertTrue(text.contains("This is another paragraph."));
    }
}
