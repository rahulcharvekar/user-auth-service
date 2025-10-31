package com.example.userauth.controller;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.EndpointPolicy;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.service.AuthorizationService;
import com.example.userauth.service.PolicyEngineService;
import com.example.userauth.service.dto.AuthorizationMatrix;
import com.example.userauth.service.dto.EndpointAuthorizationMetadata;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Internal-only endpoints that expose authorization data for downstream services.
 * These routes are guarded via internal network/API key strategies, so they skip JWT.
 */
@RestController
@RequestMapping("/internal/authz")
public class InternalAuthorizationController {

    private static final Logger logger = LoggerFactory.getLogger(InternalAuthorizationController.class);

    private final AuthorizationService authorizationService;
    private final PolicyEngineService policyEngineService;
    private final EndpointRepository endpointRepository;

    public InternalAuthorizationController(AuthorizationService authorizationService,
                                           PolicyEngineService policyEngineService,
                                           EndpointRepository endpointRepository) {
        this.authorizationService = authorizationService;
        this.policyEngineService = policyEngineService;
        this.endpointRepository = endpointRepository;
    }

    /**
     * Convenience endpoint to inspect a cataloged endpoint by id.
     */
    @GetMapping("/endpoints/{endpointId}")
    public ResponseEntity<Map<String, Object>> getEndpointById(@PathVariable Long endpointId) {
        return endpointRepository.findById(endpointId)
                .map(endpoint -> ResponseEntity.ok(toEndpointResponse(endpoint)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Resolve the authorization matrix for a user (roles and capabilities).
     */
    @GetMapping("/users/{userId}/matrix")
    public ResponseEntity<AuthorizationMatrix> getAuthorizationMatrix(@PathVariable Long userId) {
        logger.debug("Internal request for authorization matrix of user {}", userId);
        AuthorizationMatrix matrix = authorizationService.buildAuthorizationMatrix(userId);
        return ResponseEntity.ok(matrix);
    }

    private Map<String, Object> toEndpointResponse(Endpoint endpoint) {
        Map<String, Object> response = Map.of(
                "id", endpoint.getId(),
                "service", endpoint.getService(),
                "version", endpoint.getVersion(),
                "method", endpoint.getMethod(),
                "path", endpoint.getPath(),
                "description", endpoint.getDescription(),
                "isActive", endpoint.getIsActive(),
                "policyIds", endpoint.getEndpointPolicies().stream()
                        .map(EndpointPolicy::getPolicy)
                        .filter(policy -> policy != null)
                        .map(policy -> Map.of(
                                "id", policy.getId(),
                                "name", policy.getName(),
                                "type", policy.getType()))
                        .collect(Collectors.toList())
        );
        return response;
    }

    /**
     * Resolve endpoint authorization metadata for a given HTTP method + path combination.
     * Method should be upper/lower case agnostic and path should be raw request URI.
     */
    @GetMapping("/endpoints/metadata")
    public ResponseEntity<EndpointAuthorizationMetadata> getEndpointMetadata(
            @RequestParam("method") @NotBlank String method,
            @RequestParam("path") @NotBlank String path) {
        EndpointAuthorizationMetadata metadata = authorizationService.getEndpointAuthorizationMetadata(method, path);
        return ResponseEntity.ok(metadata);
    }

    /**
     * Optional policy evaluation endpoint. Downstream callers can supply endpoint id
     * plus the caller's roles to determine whether any linked policy grants access.
     */
    @PostMapping("/policies/evaluate")
    public ResponseEntity<Map<String, Object>> evaluateEndpointPolicy(@RequestBody PolicyEvaluationRequest request) {
        if (request.getEndpointId() == null) {
            throw new IllegalArgumentException("endpointId is required");
        }
        Set<String> roles = request.getRoles() != null ? request.getRoles() : Set.of();
        boolean allowed = policyEngineService.evaluateEndpointAccess(request.getEndpointId(), roles);
        Map<String, Object> response = Map.of(
                "endpointId", request.getEndpointId(),
                "allowed", allowed
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Request body for policy evaluation endpoint.
     */
    public static class PolicyEvaluationRequest {
        private Long endpointId;
        private Set<String> roles;

        public Long getEndpointId() {
            return endpointId;
        }

        public void setEndpointId(Long endpointId) {
            this.endpointId = endpointId;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }
    }
}
