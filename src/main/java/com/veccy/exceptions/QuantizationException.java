package com.veccy.exceptions;

/**
 * Exception raised for quantization-related errors.
 */
public class QuantizationException extends VeccyException {

    public QuantizationException() {
        super();
    }

    public QuantizationException(String message) {
        super(message);
    }

    public QuantizationException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuantizationException(Throwable cause) {
        super(cause);
    }
}
