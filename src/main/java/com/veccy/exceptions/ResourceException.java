package com.veccy.exceptions;

/**
 * Exception raised for resource-related errors.
 */
public class ResourceException extends VeccyException {

    public ResourceException() {
        super();
    }

    public ResourceException(String message) {
        super(message);
    }

    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceException(Throwable cause) {
        super(cause);
    }
}
