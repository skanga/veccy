package com.veccy.exceptions;

/**
 * Base exception class for all Veccy-related errors.
 * This is an unchecked exception to provide flexibility in error handling.
 */
public class VeccyException extends RuntimeException {

    public VeccyException() {
        super();
    }

    public VeccyException(String message) {
        super(message);
    }

    public VeccyException(String message, Throwable cause) {
        super(message, cause);
    }

    public VeccyException(Throwable cause) {
        super(cause);
    }
}
