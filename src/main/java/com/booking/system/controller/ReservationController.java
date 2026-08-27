package com.booking.system.controller;

import com.booking.system.dto.request.ReservationRequest;
import com.booking.system.dto.request.ReservationUpdateRequest;
import com.booking.system.dto.response.PagedResponse;
import com.booking.system.dto.response.ReservationResponse;
import com.booking.system.entity.User;
import com.booking.system.enums.ReservationStatus;
import com.booking.system.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Reservation visibility rule lives here at the query-shaping level:
 *   - ADMIN calling GET /api/reservations sees everyone's reservations.
 *   - USER calling the same endpoint is transparently scoped to their own
 *     (we pass their own id as the `ownerId` filter) - there is no way for
 *     a USER to request another user's reservations via this endpoint.
 * Row-level ownership for single-resource operations (get/update/delete by
 * id) is enforced again in ReservationService, so even guessing another
 * user's reservation id doesn't leak data.
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservations", description = "Bookings for resources")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a reservation", description =
            "The reservation owner is always the authenticated caller - it cannot be set via the request body.")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication
    ) {
        User currentUser = currentUser(authentication);
        ReservationResponse created = reservationService.createReservation(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List reservations (paginated, filterable)", description =
            "ADMIN sees all reservations. USER sees only their own. " +
            "Supports filtering by status, minPrice, maxPrice, and standard page/size/sort parameters.")
    public ResponseEntity<PagedResponse<ReservationResponse>> getReservations(
            @Parameter(description = "Filter by reservation status") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum price (inclusive)") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price (inclusive)") @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            Authentication authentication
    ) {
        User currentUser = currentUser(authentication);
        Long ownerFilter = isAdmin(authentication) ? null : currentUser.getId();

        return ResponseEntity.ok(
                reservationService.getReservations(ownerFilter, status, minPrice, maxPrice, pageable)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a reservation by id", description =
            "ADMIN can fetch any reservation. USER can only fetch their own (403 otherwise).")
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(reservationService.getReservationById(id, currentUser(authentication)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reservation", description =
            "ADMIN may update any field on any reservation. A USER may only cancel their own reservation.")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                reservationService.updateReservation(id, request, currentUser(authentication))
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a reservation", description =
            "ADMIN can delete any reservation. USER can only delete their own.")
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long id,
            Authentication authentication
    ) {
        reservationService.deleteReservation(id, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }
}
