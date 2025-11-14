package com.veccy.processing.parsers;

import com.veccy.exceptions.ParsingException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parser for HTML documents using JSoup.
 * <p>
 * Extracts text content while preserving structure, and extracts
 * metadata from HTML meta tags (title, description, keywords, etc.).
 */
public class HTMLParser implements DocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(HTMLParser.class);

    private static final String[] SUPPORTED_EXTENSIONS = {"html", "htm", "xhtml"};

    @Override
    public ParsedDocument parse(InputStream input) throws IOException {
        try {
            Document doc = Jsoup.parse(input, StandardCharsets.UTF_8.name(), "");
            return extractContent(doc, null);
        } catch (Exception e) {
            throw new ParsingException("Failed to parse HTML document: " + e.getMessage(), e);
        }
    }

    @Override
    public ParsedDocument parse(Path filePath) throws IOException {
        try {
            if (!Files.exists(filePath)) {
                throw new ParsingException("File not found: " + filePath);
            }

            Document doc = Jsoup.parse(filePath.toFile(), StandardCharsets.UTF_8.name());
            return extractContent(doc, filePath);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingException("Failed to parse HTML file: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text and metadata from a JSoup Document.
     */
    private ParsedDocument extractContent(Document doc, Path filePath) throws IOException {
        // Extract text content (body text only, excluding scripts and styles)
        doc.select("script, style, nav, footer, header").remove();
        String text = doc.body().text();

        // Extract metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parser", "HTMLParser");
        metadata.put("format", "text/html");

        if (filePath != null) {
            metadata.put("filename", filePath.getFileName().toString());
            metadata.put("file_size", Files.size(filePath));
        }

        // Title
        String title = doc.title();
        if (title != null && !title.trim().isEmpty()) {
            metadata.put("title", title);
        }

        // Meta tags
        extractMetaTags(doc, metadata);

        // Headings
        Elements h1s = doc.select("h1");
        if (!h1s.isEmpty()) {
            metadata.put("h1_headings", h1s.stream()
                    .map(Element::text)
                    .collect(Collectors.toList()));
        }

        // Links
        Elements links = doc.select("a[href]");
        metadata.put("link_count", links.size());

        // Images
        Elements images = doc.select("img[src]");
        metadata.put("image_count", images.size());

        // Text statistics
        metadata.put("character_count", text.length());
        metadata.put("word_count", countWords(text));

        // Language
        String lang = doc.select("html").attr("lang");
        if (lang != null && !lang.trim().isEmpty()) {
            metadata.put("language", lang);
        }

        logger.debug("Parsed HTML: {} characters, {} links, {} images",
                text.length(), links.size(), images.size());

        return new ParsedDocument(text, metadata);
    }

    /**
     * Extracts metadata from HTML meta tags.
     */
    private void extractMetaTags(Document doc, Map<String, Object> metadata) {
        // Description
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null) {
            String description = metaDesc.attr("content");
            if (!description.isEmpty()) {
                metadata.put("description", description);
            }
        }

        // Keywords
        Element metaKeywords = doc.selectFirst("meta[name=keywords]");
        if (metaKeywords != null) {
            String keywords = metaKeywords.attr("content");
            if (!keywords.isEmpty()) {
                metadata.put("keywords", keywords);
            }
        }

        // Author
        Element metaAuthor = doc.selectFirst("meta[name=author]");
        if (metaAuthor != null) {
            String author = metaAuthor.attr("content");
            if (!author.isEmpty()) {
                metadata.put("author", author);
            }
        }

        // Open Graph tags (for social media)
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null) {
            String title = ogTitle.attr("content");
            if (!title.isEmpty()) {
                metadata.put("og_title", title);
            }
        }

        Element ogDesc = doc.selectFirst("meta[property=og:description]");
        if (ogDesc != null) {
            String description = ogDesc.attr("content");
            if (!description.isEmpty()) {
                metadata.put("og_description", description);
            }
        }

        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage != null) {
            String image = ogImage.attr("content");
            if (!image.isEmpty()) {
                metadata.put("og_image", image);
            }
        }

        // Twitter Card tags
        Element twitterTitle = doc.selectFirst("meta[name=twitter:title]");
        if (twitterTitle != null) {
            String title = twitterTitle.attr("content");
            if (!title.isEmpty()) {
                metadata.put("twitter_title", title);
            }
        }

        Element twitterDesc = doc.selectFirst("meta[name=twitter:description]");
        if (twitterDesc != null) {
            String description = twitterDesc.attr("content");
            if (!description.isEmpty()) {
                metadata.put("twitter_description", description);
            }
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
        return "HTML Parser";
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
