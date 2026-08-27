package com.booking.system.exception;

/**
 * Thrown on unique-constraint style conflicts (e.g. username/email already taken).
 * Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
