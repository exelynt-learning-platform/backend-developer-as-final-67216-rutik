package com.booking.system.dto.request;

import com.booking.system.enums.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * All fields optional (partial update / PATCH semantics). Fields left null
 * are left unchanged. Used by ADMIN to fully manage a reservation, and by
 * the reservation owner for the more limited "cancel my own reservation"
 * flow (see ReservationService).
 */
public record ReservationUpdateRequest(
        LocalDateTime startTime,

        LocalDateTime endTime,

        ReservationStatus status,

        @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
        @Digits(integer = 8, fraction = 2, message = "Price may have at most 2 decimal places")
        BigDecimal price,

        @Size(max = 500, message = "Notes must be at most 500 characters")
        String notes
) {
}
