package com.example.userauth.service;

import com.example.userauth.entity.Policy;
import com.example.userauth.repository.PolicyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Policy Engine Service - Evaluates RBAC policies for authorization decisions
 */
@Service
public class PolicyEngineService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyEngineService.class);
    
    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;

    public PolicyEngineService(PolicyRepository policyRepository, ObjectMapper objectMapper) {
        this.policyRepository = policyRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluate if user with given roles can access an endpoint
     * 
     * @param endpointId The endpoint to check access for
     * @param userRoles The roles the user has
     * @return true if access is granted, false otherwise
     */
    public boolean evaluateEndpointAccess(Long endpointId, Set<String> userRoles) {
        List<Policy> policies = policyRepository.findByEndpointId(endpointId);
        
        if (policies.isEmpty()) {
            logger.warn("No policies found for endpoint ID: {}", endpointId);
            return false;
        }

        // Evaluate each policy - if ANY policy grants access, allow
        for (Policy policy : policies) {
            if (evaluatePolicy(policy, userRoles)) {
                logger.debug("Access granted by policy: {} for endpoint: {}", policy.getName(), endpointId);
                return true;
            }
        }

        logger.debug("Access denied for endpoint: {} with roles: {}", endpointId, userRoles);
        return false;
    }

    /**
     * Evaluate if user with given roles satisfies a page access policy
     * Pages don't have direct policy links - they have actions with required capabilities
     * This method checks if user has any capabilities for the page's actions
     * 
     * @param pageId The UI page to check access for
     * @param userRoles The roles the user has
     * @return true if access is granted, false otherwise
     */
    public boolean evaluatePageAccess(Long pageId, Set<String> userRoles) {
        // For now, allow all authenticated users to access pages
        // Real authorization happens at the action level via capabilities
        logger.debug("Page access evaluation for pageId: {} with roles: {}", pageId, userRoles);
        return !userRoles.isEmpty();
    }

    /**
     * Evaluate a single policy against user roles
     * 
     * @param policy The policy to evaluate
     * @param userRoles The roles the user has
     * @return true if policy grants access, false otherwise
     */
    private boolean evaluatePolicy(Policy policy, Set<String> userRoles) {
        try {
            JsonNode policyExpression = objectMapper.readTree(policy.getExpression());
            
            // Handle RBAC policy type
            if ("RBAC".equalsIgnoreCase(policy.getType())) {
                return evaluateRBACPolicy(policyExpression, userRoles);
            }
            
            // Handle ABAC policy type (future enhancement)
            if ("ABAC".equalsIgnoreCase(policy.getType())) {
                logger.warn("ABAC policies not yet implemented, defaulting to deny");
                return false;
            }
            
            logger.warn("Unknown policy type: {}", policy.getType());
            return false;
            
        } catch (Exception e) {
            logger.error("Error evaluating policy: {}", policy.getName(), e);
            return false;
        }
    }

    /**
     * Evaluate RBAC policy expression
     * Expected format: {"roles": ["ADMIN", "RECONCILIATION_OFFICER"]}
     * Logic: User must have at least one of the specified roles (OR logic)
     * 
     * @param policyExpression The JSON policy expression
     * @param userRoles The roles the user has
     * @return true if user has any of the required roles
     */
    private boolean evaluateRBACPolicy(JsonNode policyExpression, Set<String> userRoles) {
        if (!policyExpression.has("roles")) {
            logger.warn("RBAC policy missing 'roles' field");
            return false;
        }

        JsonNode rolesNode = policyExpression.get("roles");
        if (!rolesNode.isArray()) {
            logger.warn("RBAC policy 'roles' field is not an array");
            return false;
        }

        // Check if user has ANY of the required roles (OR logic)
        for (JsonNode roleNode : rolesNode) {
            String requiredRole = roleNode.asText();
            if (userRoles.contains(requiredRole)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get all policies for a specific endpoint
     * 
     * @param endpointId The endpoint ID
     * @return List of policies
     */
    public List<Policy> getPoliciesForEndpoint(Long endpointId) {
        return policyRepository.findByEndpointId(endpointId);
    }

    /**
     * Get all active policies
     * 
     * @return List of all active policies
     */
    public List<Policy> getAllActivePolicies() {
        return policyRepository.findByIsActiveTrue();
    }
}
