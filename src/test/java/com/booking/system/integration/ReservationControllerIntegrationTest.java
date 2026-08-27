package com.booking.system.integration;

import com.booking.system.dto.request.ReservationRequest;
import com.booking.system.dto.request.ReservationUpdateRequest;
import com.booking.system.entity.Reservation;
import com.booking.system.entity.Resource;
import com.booking.system.enums.ReservationStatus;
import com.booking.system.enums.ResourceType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservationControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void createReservation_attachesCallerAsOwner_ignoringAnyClientSuppliedIdentity() throws Exception {
        Resource resource = resourceRepository.save(sampleResource());

        ReservationRequest request = new ReservationRequest(
                resource.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                new BigDecimal("30.00"),
                "test booking"
        );

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(regularUser.getUsername()))
                .andExpect(jsonPath("$.userId").value(regularUser.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createReservation_rejectsEndBeforeStart() throws Exception {
        Resource resource = resourceRepository.save(sampleResource());

        ReservationRequest request = new ReservationRequest(
                resource.getId(),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1),
                new BigDecimal("30.00"),
                null
        );

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservation_rejectsMissingRequiredFields() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.resourceId").exists())
                .andExpect(jsonPath("$.details.price").exists());
    }

    @Test
    void listReservations_userSeesOnlyOwnReservations() throws Exception {
        Resource resource = resourceRepository.save(sampleResource());
        reservationRepository.save(reservationFor(regularUser, resource, new BigDecimal("10.00")));
        reservationRepository.save(reservationFor(secondRegularUser, resource, new BigDecimal("20.00")));

        mockMvc.perform(get("/api/reservations").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value(regularUser.getUsername()));
    }

    @Test
    void listReservations_adminSeesAllReservations() throws Exception {
        Resource resource = resourceRepository.save(sampleResource());
        reservationRepository.save(reservationFor(regularUser, resource, new BigDecimal("10.00")));
        reservationRepository.save(reservationFor(secondRegularUser, resource, new BigDecimal("20.00")));

        mockMvc.perform(get("/api/reservations").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listReservations_filtersByStatusAndPriceRange() throws Exception {
        Resource resource = resourceRepository.save(sampleResource());
        Reservation pendingCheap = reservationFor(regularUser, resource, new BigDecimal("10.00"));
        pendingCheap.setStatus(ReservationStatus.PENDING);
        Reservation confirmedExpensive = reservationFor(regularUser, resource, new BigDecimal("500.00"));
        confirmedExpensive.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(pendingCheap);
        reservationRepository.save(confirmedExpensive);

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .param("minPrice", "100")
                        .param("maxPrice", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(500.00));
    }

    @Test
    void listReservations_supportsPagination() throws Exception {
        Resource resource = resourceRepository.save(sampleResource());
        for (int i = 0; i < 5; i++) {
            reservationRepository.save(reservationFor(regularUser, resource, new BigDecimal("10.00")));
        }

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getReservationById_ownerCanAccess_otherUserGetsForbidden() throws Exception {
        Resource resource = resourceRepository.save(sampleResource());
        Reservation reservation = reservationRepository.save(reservationFor(regularUser, resource, new BigDecimal("15.00")));

        mockMvc.perform(get("/api/reservations/" + reservation.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reservations/" + reservation.getId())
                        .header("Authorization", bearer(secondUserToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reservations/" + reservation.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void updateReservation_userCanCancelOwnButNotChangePrice() throws Exception {
        Resource resource = resourceRepository.save(sampleResource());
        Reservation reservation = reservationRepository.save(reservationFor(regularUser, resource, new BigDecimal("15.00")));

        ReservationUpdateRequest cancel = new ReservationUpdateRequest(null, null, ReservationStatus.CANCELLED, null, null);
        mockMvc.perform(put("/api/reservations/" + reservation.getId())
                        .header("Authorization", bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cancel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        ReservationUpdateRequest priceChange = new ReservationUpdateRequest(null, null, null, new BigDecimal("999.00"), null);
        mockMvc.perform(put("/api/reservations/" + reservation.getId())
                        .header("Authorization", bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(priceChange)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReservation_adminCanConfirmAndChangePrice() throws Exception {
        Resource resource = resourceRepository.save(sampleResource());
        Reservation reservation = reservationRepository.save(reservationFor(regularUser, resource, new BigDecimal("15.00")));

        ReservationUpdateRequest confirm = new ReservationUpdateRequest(
                null, null, ReservationStatus.CONFIRMED, new BigDecimal("45.00"), "approved"
        );

        mockMvc.perform(put("/api/reservations/" + reservation.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(confirm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.price").value(45.00));
    }

    @Test
    void deleteReservation_otherUserForbidden_ownerAllowed() throws Exception {
        Resource resource = resourceRepository.save(sampleResource());
        Reservation reservation = reservationRepository.save(reservationFor(regularUser, resource, new BigDecimal("15.00")));

        mockMvc.perform(delete("/api/reservations/" + reservation.getId())
                        .header("Authorization", bearer(secondUserToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reservations/" + reservation.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void reservationEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidOrExpiredToken_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    private Resource sampleResource() {
        return Resource.builder()
                .name("Bookable Room")
                .description("desc")
                .type(ResourceType.ROOM)
                .location("Floor 2")
                .available(true)
                .build();
    }

    private Reservation reservationFor(com.booking.system.entity.User user, Resource resource, BigDecimal price) {
        return Reservation.builder()
                .resource(resource)
                .user(user)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(ReservationStatus.PENDING)
                .price(price)
                .build();
    }
}
