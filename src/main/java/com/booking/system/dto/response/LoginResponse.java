package com.booking.system.dto.response;

public record LoginResponse(
        String token,
        String tokenType,
        String username,
        String role,
        long expiresInMs
) {
    public static LoginResponse of(String token, String username, String role, long expiresInMs) {
        return new LoginResponse(token, "Bearer", username, role, expiresInMs);
    }
}
