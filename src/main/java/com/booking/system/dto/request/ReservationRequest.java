package com.booking.system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Note there is deliberately no `userId` / `username` field here.
 * The reservation owner is always resolved server-side from the
 * authenticated JWT principal (see ReservationService#createReservation),
 * so a client can never book on behalf of someone else.
 */
public record ReservationRequest(
        @NotNull(message = "Resource id is required")
        Long resourceId,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        LocalDateTime startTime,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        LocalDateTime endTime,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
        @Digits(integer = 8, fraction = 2, message = "Price may have at most 2 decimal places")
        BigDecimal price,

        @Size(max = 500, message = "Notes must be at most 500 characters")
        String notes
) {
}
