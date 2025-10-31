package com.example.userauth.security;

import com.example.userauth.entity.User;
import com.example.userauth.service.AuthorizationService;
import com.example.userauth.service.PolicyEngineService;
import com.example.userauth.service.dto.AuthorizationMatrix;
import com.example.userauth.service.dto.EndpointAuthorizationMetadata;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;

/**
 * AuthorizationManager that enforces RBAC policies defined in the service catalog.
 * It reuses the same authorization matrix exposed via /api/me/authorizations.
 */
@Component
public class DynamicEndpointAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Logger logger = LoggerFactory.getLogger(DynamicEndpointAuthorizationManager.class);

    private final AuthorizationService authorizationService;
    private final PolicyEngineService policyEngineService;

    public DynamicEndpointAuthorizationManager(AuthorizationService authorizationService,
                                               PolicyEngineService policyEngineService) {
        this.authorizationService = authorizationService;
        this.policyEngineService = policyEngineService;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier,
                                       RequestAuthorizationContext requestContext) {
        HttpServletRequest request = requestContext.getRequest();
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return new AuthorizationDecision(true);
        }

        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        EndpointAuthorizationMetadata metadata =
                authorizationService.getEndpointAuthorizationMetadata(method, resolvePath(request));

        if (!metadata.isEndpointFound()) {
            // Endpoint not cataloged: fail closed so every backend route must be registered with a policy
            logger.warn("No cataloged endpoint for {} {}, denying access by default", method, request.getRequestURI());
            return new AuthorizationDecision(false);
        }

        if (!metadata.hasPolicies()) {
            logger.warn("Endpoint {} matched for {} {} but no policies assigned. Denying access.",
                    metadata.getEndpointId(), method, request.getRequestURI());
            return new AuthorizationDecision(false);
        }

        Long userId = extractUserId(authentication.getPrincipal());
        if (userId == null) {
            logger.warn("Unable to extract user id from principal {} for {} {}, denying",
                    authentication.getPrincipal().getClass().getName(), method, request.getRequestURI());
            return new AuthorizationDecision(false);
        }

        AuthorizationMatrix matrix = authorizationService.buildAuthorizationMatrix(userId);

        Set<String> requiredCapabilities = metadata.getRequiredCapabilities();
        boolean allowed;

        if (!requiredCapabilities.isEmpty()) {
            allowed = requiredCapabilities.stream().anyMatch(matrix.getCapabilities()::contains);
            if (!allowed) {
                logger.debug("Denied {} {} for user {} - missing capabilities {} (has {})",
                        method, request.getRequestURI(), userId, requiredCapabilities, matrix.getCapabilities());
            }
        } else {
            // Fallback to policy expression evaluation when no capabilities are mapped
            allowed = policyEngineService.evaluateEndpointAccess(metadata.getEndpointId(), matrix.getRoles());
            if (!allowed) {
                logger.debug("Denied {} {} for user {} - policy expressions not satisfied (roles: {})",
                        method, request.getRequestURI(), userId, matrix.getRoles());
            }
        }

        if (allowed) {
            logger.trace("Authorized {} {} for user {}", method, request.getRequestURI(), userId);
        }

        return new AuthorizationDecision(allowed);
    }

    private Long extractUserId(Object principal) {
        if (principal instanceof User user) {
            return user.getId();
        }
        if (principal instanceof String username) {
            // username-only principals are unexpected in this service; log and deny
            logger.warn("Authentication principal is username string '{}', cannot resolve user id", username);
        }
        return null;
    }

    private String resolvePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        return uri;
    }
}
