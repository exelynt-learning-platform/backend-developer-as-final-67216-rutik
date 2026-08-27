package com.booking.system.enums;

/**
 * Application roles used for RBAC.
 * Spring Security expects authorities prefixed with "ROLE_"; that prefix
 * is applied where the authority is constructed (see User#getAuthorities).
 */
public enum Role {
    ADMIN,
    USER
}
