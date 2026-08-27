package com.booking.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the `app.jwt.*` properties from application.yml / environment variables.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {
}
