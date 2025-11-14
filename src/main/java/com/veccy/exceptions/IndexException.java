package com.veccy.exceptions;

/**
 * Exception raised for index-related errors.
 */
public class IndexException extends VeccyException {

    public IndexException() {
        super();
    }

    public IndexException(String message) {
        super(message);
    }

    public IndexException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexException(Throwable cause) {
        super(cause);
    }
}
