package com.booking.system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ResourceBookingSystemApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full Spring context (security, JPA, JWT config, etc.)
        // wires together correctly.
    }
}
