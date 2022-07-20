package com.architecture.first.framework.business.actors.exceptions;

/**
 * Exception thrown when an Actor has a problem during memory processing
 */
public class MemoryException extends RuntimeException {
    public MemoryException(Throwable cause) {
        super(cause);
    }
}
