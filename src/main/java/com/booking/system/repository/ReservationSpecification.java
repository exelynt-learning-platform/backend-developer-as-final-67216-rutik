package com.booking.system.repository;

import com.booking.system.entity.Reservation;
import com.booking.system.enums.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Builds a composable JPA Specification for reservation queries.
 * Each filter is applied only when its corresponding argument is non-null,
 * so callers can mix and match status / minPrice / maxPrice / ownerId freely.
 */
public final class ReservationSpecification {

    private ReservationSpecification() {
    }

    public static Specification<Reservation> withFilters(
            Long ownerId,
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (ownerId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("user").get("id"), ownerId));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return predicate;
        };
    }
}
