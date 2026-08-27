package com.booking.system.service;

import com.booking.system.dto.request.ReservationRequest;
import com.booking.system.dto.request.ReservationUpdateRequest;
import com.booking.system.dto.response.PagedResponse;
import com.booking.system.dto.response.ReservationResponse;
import com.booking.system.entity.Reservation;
import com.booking.system.entity.Resource;
import com.booking.system.entity.User;
import com.booking.system.enums.ReservationStatus;
import com.booking.system.exception.InvalidReservationException;
import com.booking.system.exception.ResourceNotFoundException;
import com.booking.system.repository.ReservationRepository;
import com.booking.system.repository.ReservationSpecification;
import com.booking.system.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Owns all reservation business rules, in particular the ownership model:
 *   - Creation always attaches the authenticated principal as the owner;
 *     the request body has no user/owner field, so there's nothing for a
 *     client to spoof.
 *   - Reads/updates/deletes check that the caller is either the owner or
 *     an ADMIN before touching the row - a USER requesting someone else's
 *     reservation id gets 403/404 (see getReservationById), never the data.
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, User currentUser) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidReservationException("End time must be after start time");
        }

        Resource resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + request.resourceId()));

        if (!resource.isAvailable()) {
            throw new InvalidReservationException("Resource is not available for booking: " + resource.getName());
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(currentUser) // <-- ownership always derived from the JWT principal, never the request body
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(ReservationStatus.PENDING)
                .price(request.price())
                .notes(request.notes())
                .build();

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    /**
     * ADMIN sees all reservations; a USER only ever sees their own.
     * The `ownerId` filter is set by the controller to currentUser.getId()
     * for USER callers, and left null (no filter) for ADMIN callers.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> getReservations(
            Long ownerId,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidReservationException("minPrice must not be greater than maxPrice");
        }

        var spec = ReservationSpecification.withFilters(ownerId, status, minPrice, maxPrice);
        Page<ReservationResponse> page = reservationRepository.findAll(spec, pageable)
                .map(ReservationResponse::from);

        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, User currentUser) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnerOrAdmin(reservation, currentUser);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationUpdateRequest request, User currentUser) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnerOrAdmin(reservation, currentUser);

        boolean isAdmin = isAdmin(currentUser);

        // A non-admin owner may only cancel their own reservation - they cannot
        // change price, times, or move it to CONFIRMED (that's an admin action).
        if (!isAdmin) {
            boolean onlyCancelling = request.status() == ReservationStatus.CANCELLED
                    && request.startTime() == null
                    && request.endTime() == null
                    && request.price() == null;
            if (!onlyCancelling) {
                throw new AccessDeniedException("Users may only cancel their own reservations");
            }
            reservation.setStatus(ReservationStatus.CANCELLED);
            if (request.notes() != null) {
                reservation.setNotes(request.notes());
            }
            return ReservationResponse.from(reservationRepository.save(reservation));
        }

        // Admin: full partial update.
        if (request.startTime() != null) {
            reservation.setStartTime(request.startTime());
        }
        if (request.endTime() != null) {
            reservation.setEndTime(request.endTime());
        }
        if (request.startTime() != null || request.endTime() != null) {
            if (!reservation.getEndTime().isAfter(reservation.getStartTime())) {
                throw new InvalidReservationException("End time must be after start time");
            }
        }
        if (request.status() != null) {
            reservation.setStatus(request.status());
        }
        if (request.price() != null) {
            reservation.setPrice(request.price());
        }
        if (request.notes() != null) {
            reservation.setNotes(request.notes());
        }

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public void deleteReservation(Long id, User currentUser) {
        Reservation reservation = findReservationOrThrow(id);
        assertOwnerOrAdmin(reservation, currentUser);
        reservationRepository.delete(reservation);
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private void assertOwnerOrAdmin(Reservation reservation, User currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            // Deliberately the same exception a truly-missing id would produce upstream
            // (mapped to 404) is NOT used here - a 403 is appropriate since the caller
            // is authenticated, just not entitled to this particular reservation.
            throw new AccessDeniedException("You do not have permission to access this reservation");
        }
    }

    private boolean isAdmin(User user) {
        return user.getRole().name().equals("ADMIN");
    }
}
