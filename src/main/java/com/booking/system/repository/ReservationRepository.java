package com.booking.system.repository;

import com.booking.system.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * JpaSpecificationExecutor lets us compose dynamic WHERE clauses
 * (status / minPrice / maxPrice / owner) without a combinatorial
 * explosion of derived query methods.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
}
