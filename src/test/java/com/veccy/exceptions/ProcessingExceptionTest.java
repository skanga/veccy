package com.veccy.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProcessingException.
 */
class ProcessingExceptionTest {

    @Test
    void testDefaultConstructor() {
        ProcessingException exception = new ProcessingException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void testMessageConstructor() {
        String message = "Document processing failed";
        ProcessingException exception = new ProcessingException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Embedding generation failed";
        RuntimeException cause = new RuntimeException("Model not loaded");
        ProcessingException exception = new ProcessingException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCauseConstructor() {
        OutOfMemoryError cause = new OutOfMemoryError("Cannot allocate tensor");
        ProcessingException exception = new ProcessingException(cause);

        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritance() {
        ProcessingException exception = new ProcessingException("test");

        assertInstanceOf(VeccyException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(ProcessingException.class, () -> {
            throw new ProcessingException("Processing error");
        });
    }

    @Test
    void testTypicalProcessingErrors() {
        // Pipeline failure
        ProcessingException pipelineError = new ProcessingException("Processing pipeline failed at stage 3");
        assertTrue(pipelineError.getMessage().contains("pipeline"));

        // Transformation error
        ProcessingException transformError = new ProcessingException("Document transformation failed");
        assertTrue(transformError.getMessage().contains("transformation"));

        // Extraction error
        ProcessingException extractionError = new ProcessingException("Text extraction failed");
        assertTrue(extractionError.getMessage().contains("extraction"));

        // Timeout
        ProcessingException timeoutError = new ProcessingException("Processing timeout: exceeded 30 seconds");
        assertTrue(timeoutError.getMessage().contains("timeout"));

        // Batch processing error
        ProcessingException batchError = new ProcessingException("Batch processing failed: 15/100 documents processed");
        assertTrue(batchError.getMessage().contains("Batch"));
    }

    @Test
    void testEmbeddingProcessingErrors() {
        // Model error
        ProcessingException modelError = new ProcessingException("Embedding model not initialized");
        assertTrue(modelError.getMessage().contains("model"));

        // Tokenization error
        ProcessingException tokenError = new ProcessingException("Tokenization failed: text exceeds maximum length");
        assertTrue(tokenError.getMessage().contains("Tokenization"));

        // Dimension mismatch
        ProcessingException dimensionError = new ProcessingException("Embedding dimension mismatch: expected 768, got 384");
        assertTrue(dimensionError.getMessage().contains("dimension"));

        // Inference error
        ProcessingException inferenceError = new ProcessingException("Model inference failed");
        assertTrue(inferenceError.getMessage().contains("inference"));
    }

    @Test
    void testTextProcessingErrors() {
        // Cleaning error
        ProcessingException cleanError = new ProcessingException("Text cleaning failed: invalid regex pattern");
        assertTrue(cleanError.getMessage().contains("cleaning"));

        // Normalization error
        ProcessingException normError = new ProcessingException("Text normalization failed");
        assertTrue(normError.getMessage().contains("normalization"));

        // Tokenization error
        ProcessingException tokenizationError = new ProcessingException("Failed to tokenize text");
        assertTrue(tokenizationError.getMessage().contains("tokenize"));

        // Language detection error
        ProcessingException langError = new ProcessingException("Language detection failed");
        assertTrue(langError.getMessage().contains("Language"));
    }

    @Test
    void testDocumentProcessingErrors() {
        // Content extraction
        ProcessingException contentError = new ProcessingException("Failed to extract content from PDF");
        assertTrue(contentError.getMessage().contains("extract"));

        // Metadata extraction
        ProcessingException metadataError = new ProcessingException("Metadata extraction failed");
        assertTrue(metadataError.getMessage().contains("Metadata"));

        // Chunking error
        ProcessingException chunkError = new ProcessingException("Document chunking failed: invalid chunk size");
        assertTrue(chunkError.getMessage().contains("chunking"));

        // Filtering error
        ProcessingException filterError = new ProcessingException("Document filtering failed");
        assertTrue(filterError.getMessage().contains("filtering"));
    }

    @Test
    void testBatchProcessingErrors() {
        String message = "Batch processing failed: [succeeded=85, failed=15, total=100]";
        ProcessingException exception = new ProcessingException(message);

        assertTrue(exception.getMessage().contains("succeeded"));
        assertTrue(exception.getMessage().contains("failed"));
        assertTrue(exception.getMessage().contains("total"));
    }

    @Test
    void testProcessingErrorWithProgress() {
        String message = "Processing interrupted at 45% completion";
        ProcessingException exception = new ProcessingException(message);

        assertTrue(exception.getMessage().contains("45%"));
        assertTrue(exception.getMessage().contains("interrupted"));
    }

    @Test
    void testProcessingErrorWithMultipleFailures() {
        String message = "Multiple processing failures:\n" +
                         "- Document 1: encoding error\n" +
                         "- Document 3: parsing failed\n" +
                         "- Document 7: content too large";
        ProcessingException exception = new ProcessingException(message);

        assertTrue(exception.getMessage().contains("Multiple"));
        assertTrue(exception.getMessage().contains("Document 1"));
        assertTrue(exception.getMessage().contains("Document 3"));
        assertTrue(exception.getMessage().contains("Document 7"));
    }

    @Test
    void testTFIDFProcessingError() {
        ProcessingException tfidfError = new ProcessingException("TF-IDF computation failed: corpus is empty");
        assertTrue(tfidfError.getMessage().contains("TF-IDF"));
    }

    @Test
    void testChainedProcessingErrors() {
        ParsingException parsingCause = new ParsingException("Failed to parse HTML");
        ProcessingException exception = new ProcessingException("Document processing pipeline failed", parsingCause);

        assertEquals("Document processing pipeline failed", exception.getMessage());
        assertInstanceOf(ParsingException.class, exception.getCause());
    }
}
