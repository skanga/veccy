package com.veccy.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QuantizationException.
 */
class QuantizationExceptionTest {

    @Test
    void testDefaultConstructor() {
        QuantizationException exception = new QuantizationException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void testMessageConstructor() {
        String message = "Quantization failed";
        QuantizationException exception = new QuantizationException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Scalar quantization error";
        ArithmeticException cause = new ArithmeticException("Division by zero");
        QuantizationException exception = new QuantizationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCauseConstructor() {
        IllegalStateException cause = new IllegalStateException("Quantizer not trained");
        QuantizationException exception = new QuantizationException(cause);

        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritance() {
        QuantizationException exception = new QuantizationException("test");

        assertInstanceOf(VeccyException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(QuantizationException.class, () -> {
            throw new QuantizationException("Quantization error");
        });
    }

    @Test
    void testTypicalQuantizationErrors() {
        // Not trained
        QuantizationException notTrained = new QuantizationException("Quantizer not trained");
        assertTrue(notTrained.getMessage().contains("not trained"));

        // Invalid codebook
        QuantizationException invalidCodebook = new QuantizationException("Invalid codebook size");
        assertTrue(invalidCodebook.getMessage().contains("codebook"));

        // Dimension mismatch
        QuantizationException dimensionError = new QuantizationException("Vector dimension does not match quantizer");
        assertTrue(dimensionError.getMessage().contains("dimension"));

        // Insufficient training data
        QuantizationException insufficientData = new QuantizationException("Insufficient training data");
        assertTrue(insufficientData.getMessage().contains("Insufficient"));
    }

    @Test
    void testQuantizationTypeSpecificErrors() {
        // Scalar quantization
        QuantizationException scalarError = new QuantizationException("Scalar quantization: bits must be between 1 and 8");
        assertTrue(scalarError.getMessage().contains("Scalar"));

        // Product quantization
        QuantizationException pqError = new QuantizationException("Product quantization: subvector count must divide vector dimension");
        assertTrue(pqError.getMessage().contains("Product"));

        // Clustering error
        QuantizationException clusterError = new QuantizationException("K-means clustering failed to converge");
        assertTrue(clusterError.getMessage().contains("clustering"));
    }
}
