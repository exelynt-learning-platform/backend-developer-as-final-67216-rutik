package com.booking.system.integration;

import com.booking.system.entity.User;
import com.booking.system.enums.Role;
import com.booking.system.repository.ReservationRepository;
import com.booking.system.repository.ResourceRepository;
import com.booking.system.repository.UserRepository;
import com.booking.system.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Boots the full application context (real security filter chain, real JPA)
 * against an in-memory H2 database so integration tests exercise the same
 * code path a production request would - including JWT parsing and
 * @PreAuthorize checks, not just service-layer logic in isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ResourceRepository resourceRepository;

    @Autowired
    protected ReservationRepository reservationRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    protected User adminUser;
    protected User regularUser;
    protected User secondRegularUser;

    protected String adminToken;
    protected String userToken;
    protected String secondUserToken;

    @BeforeEach
    void baseSetUp() {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .username("admin-it")
                .email("admin-it@test.local")
                .password(passwordEncoder.encode("adminpass"))
                .role(Role.ADMIN)
                .build());

        regularUser = userRepository.save(User.builder()
                .username("user-it")
                .email("user-it@test.local")
                .password(passwordEncoder.encode("userpass"))
                .role(Role.USER)
                .build());

        secondRegularUser = userRepository.save(User.builder()
                .username("user2-it")
                .email("user2-it@test.local")
                .password(passwordEncoder.encode("userpass"))
                .role(Role.USER)
                .build());

        adminToken = jwtService.generateToken(adminUser, "ADMIN");
        userToken = jwtService.generateToken(regularUser, "USER");
        secondUserToken = jwtService.generateToken(secondRegularUser, "USER");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
