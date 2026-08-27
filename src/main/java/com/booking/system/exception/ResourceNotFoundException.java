package com.booking.system.exception;

/**
 * Thrown when a requested entity (resource, reservation, user) does not exist.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
