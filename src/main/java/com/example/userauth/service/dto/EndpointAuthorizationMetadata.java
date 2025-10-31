package com.example.userauth.service.dto;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Metadata describing the authorization requirements for a cataloged endpoint.
 */
public class EndpointAuthorizationMetadata {

    private final boolean endpointFound;
    private final Long endpointId;
    private final boolean hasPolicies;
    private final Set<Long> policyIds;
    private final Set<String> requiredCapabilities;

    public EndpointAuthorizationMetadata(boolean endpointFound,
                                         Long endpointId,
                                         boolean hasPolicies,
                                         Set<Long> policyIds,
                                         Set<String> requiredCapabilities) {
        this.endpointFound = endpointFound;
        this.endpointId = endpointId;
        this.hasPolicies = hasPolicies;
        this.policyIds = policyIds != null ? Collections.unmodifiableSet(new HashSet<>(policyIds)) : Set.of();
        this.requiredCapabilities = requiredCapabilities != null
                ? Collections.unmodifiableSet(new HashSet<>(requiredCapabilities))
                : Set.of();
    }

    public boolean isEndpointFound() {
        return endpointFound;
    }

    public Long getEndpointId() {
        return endpointId;
    }

    public boolean hasPolicies() {
        return hasPolicies;
    }

    public boolean isHasPolicies() {
        return hasPolicies;
    }

    public Set<Long> getPolicyIds() {
        return policyIds;
    }

    public Set<String> getRequiredCapabilities() {
        return requiredCapabilities;
    }
}
