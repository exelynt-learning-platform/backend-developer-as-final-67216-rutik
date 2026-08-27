package com.booking.system.security;

import com.booking.system.config.JwtProperties;
import com.booking.system.entity.User;
import com.booking.system.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "unit-test-secret-key-that-is-at-least-32-bytes-long", 3600000L
        );
        jwtService = new JwtService(properties);

        testUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .password("irrelevant-hash")
                .role(Role.USER)
                .build();
    }

    @Test
    void generateToken_thenExtractUsername_roundTrips() {
        String token = jwtService.generateToken(testUser, "USER");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isTokenValid_returnsTrue_forMatchingUserAndUnexpiredToken() {
        String token = jwtService.generateToken(testUser, "USER");

        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_forDifferentUser() {
        String token = jwtService.generateToken(testUser, "USER");

        User otherUser = User.builder()
                .id(2L).username("bob").email("bob@example.com")
                .password("hash").role(Role.USER).build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forExpiredToken() throws InterruptedException {
        JwtProperties shortLived = new JwtProperties(
                "unit-test-secret-key-that-is-at-least-32-bytes-long", 1L // 1ms expiry
        );
        JwtService shortLivedService = new JwtService(shortLived);

        String token = shortLivedService.generateToken(testUser, "USER");
        Thread.sleep(10);

        assertThat(shortLivedService.isTokenValid(token, testUser)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forMalformedToken() {
        assertThat(jwtService.isTokenValid("not-a-real-jwt", testUser)).isFalse();
    }
}
