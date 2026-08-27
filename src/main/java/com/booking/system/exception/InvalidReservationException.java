package com.booking.system.exception;

/**
 * Thrown for reservation business-rule violations (e.g. end time before start
 * time, booking an unavailable resource, an overlapping time slot).
 * Mapped to HTTP 400 by GlobalExceptionHandler.
 */
public class InvalidReservationException extends RuntimeException {
    public InvalidReservationException(String message) {
        super(message);
    }
}
