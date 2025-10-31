package com.example.userauth.service.dto;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable snapshot representing the authorization state for a user.
 * This is built on-demand (no caching yet) and reused by UI and backend enforcement.
 */
public class AuthorizationMatrix {

    private final Long userId;
    private final Integer permissionVersion;
    private final Set<String> roles;
    private final Set<String> capabilities;

    public AuthorizationMatrix(Long userId, Integer permissionVersion,
                               Set<String> roles, Set<String> capabilities) {
        this.userId = userId;
        this.permissionVersion = permissionVersion;
        this.roles = roles != null ? Collections.unmodifiableSet(new HashSet<>(roles)) : Set.of();
        this.capabilities = capabilities != null ? Collections.unmodifiableSet(new HashSet<>(capabilities)) : Set.of();
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getPermissionVersion() {
        return permissionVersion;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }
}
