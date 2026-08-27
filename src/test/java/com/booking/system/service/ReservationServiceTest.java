package com.booking.system.service;

import com.booking.system.dto.request.ReservationRequest;
import com.booking.system.dto.request.ReservationUpdateRequest;
import com.booking.system.dto.response.ReservationResponse;
import com.booking.system.entity.Reservation;
import com.booking.system.entity.Resource;
import com.booking.system.entity.User;
import com.booking.system.enums.ReservationStatus;
import com.booking.system.enums.Role;
import com.booking.system.exception.InvalidReservationException;
import com.booking.system.exception.ResourceNotFoundException;
import com.booking.system.repository.ReservationRepository;
import com.booking.system.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User owner;
    private User otherUser;
    private User admin;
    private Resource resource;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").email("owner@x.com")
                .password("hash").role(Role.USER).build();
        otherUser = User.builder().id(2L).username("other").email("other@x.com")
                .password("hash").role(Role.USER).build();
        admin = User.builder().id(3L).username("admin").email("admin@x.com")
                .password("hash").role(Role.ADMIN).build();

        resource = Resource.builder().id(10L).name("Room A").type(com.booking.system.enums.ResourceType.ROOM)
                .location("Floor 1").available(true).build();
    }

    @Test
    void createReservation_attachesAuthenticatedUserAsOwner_regardlessOfRequestBody() {
        ReservationRequest request = new ReservationRequest(
                10L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                new BigDecimal("50.00"),
                "notes"
        );

        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        // The request DTO has no user/owner field at all - createReservation is
        // only ever given the authenticated principal directly.
        ReservationResponse response = reservationService.createReservation(request, owner);

        assertThat(response.userId()).isEqualTo(owner.getId());
        assertThat(response.username()).isEqualTo("owner");
        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(owner);
    }

    @Test
    void createReservation_rejectsEndTimeBeforeStartTime() {
        ReservationRequest request = new ReservationRequest(
                10L,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1), // before start
                new BigDecimal("50.00"),
                null
        );

        assertThatThrownBy(() -> reservationService.createReservation(request, owner))
                .isInstanceOf(InvalidReservationException.class);

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void createReservation_rejectsUnavailableResource() {
        resource.setAvailable(false);
        ReservationRequest request = new ReservationRequest(
                10L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                new BigDecimal("20.00"),
                null
        );
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> reservationService.createReservation(request, owner))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void createReservation_rejectsMissingResource() {
        ReservationRequest request = new ReservationRequest(
                999L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                new BigDecimal("20.00"),
                null
        );
        when(resourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(request, owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReservationById_ownerCanAccessOwnReservation() {
        Reservation reservation = buildReservation(owner);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.getReservationById(100L, owner);

        assertThat(response.id()).isEqualTo(100L);
    }

    @Test
    void getReservationById_nonOwnerNonAdminIsDenied() {
        Reservation reservation = buildReservation(owner);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationById(100L, otherUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getReservationById_adminCanAccessAnyReservation() {
        Reservation reservation = buildReservation(owner);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.getReservationById(100L, admin);

        assertThat(response.id()).isEqualTo(100L);
    }

    @Test
    void updateReservation_userCanCancelOwnReservation() {
        Reservation reservation = buildReservation(owner);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationUpdateRequest cancelRequest = new ReservationUpdateRequest(
                null, null, ReservationStatus.CANCELLED, null, null
        );

        ReservationResponse response = reservationService.updateReservation(100L, cancelRequest, owner);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void updateReservation_userCannotChangePriceOrConfirmOwnReservation() {
        Reservation reservation = buildReservation(owner);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        ReservationUpdateRequest priceChange = new ReservationUpdateRequest(
                null, null, null, new BigDecimal("999.00"), null
        );

        assertThatThrownBy(() -> reservationService.updateReservation(100L, priceChange, owner))
                .isInstanceOf(AccessDeniedException.class);

        ReservationUpdateRequest confirmAttempt = new ReservationUpdateRequest(
                null, null, ReservationStatus.CONFIRMED, null, null
        );

        assertThatThrownBy(() -> reservationService.updateReservation(100L, confirmAttempt, owner))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateReservation_userCannotModifyAnotherUsersReservation() {
        Reservation reservation = buildReservation(owner);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        ReservationUpdateRequest cancelRequest = new ReservationUpdateRequest(
                null, null, ReservationStatus.CANCELLED, null, null
        );

        assertThatThrownBy(() -> reservationService.updateReservation(100L, cancelRequest, otherUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateReservation_adminCanFullyUpdateAnyReservation() {
        Reservation reservation = buildReservation(owner);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationUpdateRequest adminUpdate = new ReservationUpdateRequest(
                null, null, ReservationStatus.CONFIRMED, new BigDecimal("75.00"), "confirmed by admin"
        );

        ReservationResponse response = reservationService.updateReservation(100L, adminUpdate, admin);

        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(response.price()).isEqualByComparingTo("75.00");
        assertThat(response.notes()).isEqualTo("confirmed by admin");
    }

    @Test
    void deleteReservation_nonOwnerNonAdminIsDenied() {
        Reservation reservation = buildReservation(owner);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.deleteReservation(100L, otherUser))
                .isInstanceOf(AccessDeniedException.class);

        verify(reservationRepository, never()).delete(any(Reservation.class));
    }

    @Test
    void deleteReservation_ownerCanDeleteOwnReservation() {
        Reservation reservation = buildReservation(owner);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        reservationService.deleteReservation(100L, owner);

        verify(reservationRepository).delete(reservation);
    }

    @Test
    void getReservations_rejectsMinPriceGreaterThanMaxPrice() {
        assertThatThrownBy(() -> reservationService.getReservations(
                owner.getId(), null, new BigDecimal("100"), new BigDecimal("10"),
                org.springframework.data.domain.PageRequest.of(0, 10)
        )).isInstanceOf(InvalidReservationException.class);
    }

    private Reservation buildReservation(User forUser) {
        return Reservation.builder()
                .id(100L)
                .resource(resource)
                .user(forUser)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(ReservationStatus.PENDING)
                .price(new BigDecimal("50.00"))
                .build();
    }
}
