package com.example.userauth.controller;

import com.example.userauth.entity.Capability;
import com.example.userauth.entity.Policy;
import com.example.userauth.entity.PolicyCapability;
import com.example.userauth.repository.CapabilityRepository;
import com.example.userauth.repository.PolicyCapabilityRepository;
import com.example.userauth.repository.PolicyRepository;
import com.example.userauth.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import com.shared.common.util.ETagUtil;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.shared.common.annotation.Auditable;

/**
 * Admin controller for managing policies and their capability assignments
 * Only accessible by ADMIN role
 */
@RestController
@RequestMapping("/api/admin/policies")
@SecurityRequirement(name = "Bearer Authentication")
public class PolicyController {

    private static final Logger logger = LoggerFactory.getLogger(PolicyController.class);

    @Autowired
    private ObjectMapper objectMapper;

    private final PolicyRepository policyRepository;
    private final CapabilityRepository capabilityRepository;
    private final PolicyCapabilityRepository policyCapabilityRepository;
    private final RoleRepository roleRepository;

    public PolicyController(
            PolicyRepository policyRepository,
            CapabilityRepository capabilityRepository,
            PolicyCapabilityRepository policyCapabilityRepository,
            RoleRepository roleRepository) {
        this.policyRepository = policyRepository;
        this.capabilityRepository = capabilityRepository;
        this.policyCapabilityRepository = policyCapabilityRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Get all policies with their capabilities
     */
    @Auditable(action = "GET_ALL_POLICIES", resourceType = "POLICY")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllPolicies(HttpServletRequest request) {
        List<Policy> policies = policyRepository.findAll();
        List<Map<String, Object>> response = policies.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(response);
        } catch (Exception e) {
            logger.error("Error processing policies response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get policy by ID with capabilities
     */
    @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> getPolicyById(@PathVariable Long id, HttpServletRequest request) {
        return policyRepository.findById(id)
                .map(policy -> {
                    Map<String, Object> response = convertToResponse(policy);
                    try {
                        String responseJson = objectMapper.writeValueAsString(response);
                        String eTag = ETagUtil.generateETag(responseJson);
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (eTag.equals(ifNoneMatch)) {
                            return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.status(304).eTag(eTag).build();
                        }
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.ok().eTag(eTag).body(response);
                    } catch (Exception e) {
                        logger.error("Error processing policy response", e);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new policy
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createPolicy(@RequestBody PolicyRequest request) {
        // Validate roles in expression if it's RBAC type
        if ("RBAC".equalsIgnoreCase(request.getType()) || request.getType() == null) {
            validateRolesInExpression(request.getExpression());
        }

        Policy policy = new Policy(
                request.getName(),
                request.getDescription(),
                request.getType() != null ? request.getType() : "ROLE_BASED",
                request.getExpression()
        );
        policy.setIsActive(request.getIsActive());
        Policy saved = policyRepository.save(policy);
        
        // Assign capabilities if provided
        if (request.getCapabilityIds() != null && !request.getCapabilityIds().isEmpty()) {
            assignCapabilities(saved.getId(), request.getCapabilityIds());
        }
        
        return ResponseEntity.ok(convertToResponse(policyRepository.findById(saved.getId()).get()));
    }

    /**
     * Validate that all roles in the policy expression exist in the database
     */
    private void validateRolesInExpression(String expression) {
        try {
            JsonNode policyExpression = objectMapper.readTree(expression);
            if (policyExpression.has("roles") && policyExpression.get("roles").isArray()) {
                for (JsonNode roleNode : policyExpression.get("roles")) {
                    String roleName = roleNode.asText();
                    if (!roleRepository.existsByName(roleName)) {
                        throw new IllegalArgumentException("Role '" + roleName + "' does not exist");
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid policy expression or role not found: " + e.getMessage());
        }
    }

    /**
     * Update policy
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updatePolicy(
            @PathVariable Long id,
            @RequestBody PolicyRequest request) {
        
        return policyRepository.findById(id)
                .map(policy -> {
                    policy.setName(request.getName());
                    policy.setDescription(request.getDescription());
                    if (request.getType() != null) {
                        policy.setType(request.getType());
                    }
                    // Validate roles in expression if it's RBAC type
                    if ("RBAC".equalsIgnoreCase(request.getType()) || request.getType() == null) {
                        validateRolesInExpression(request.getExpression());
                    }
                    policy.setExpression(request.getExpression());
                    policy.setIsActive(request.getIsActive());
                    policyRepository.save(policy);
                    
                    // Update capabilities if provided
                    if (request.getCapabilityIds() != null) {
                        // Remove existing capabilities
                        policyCapabilityRepository.deleteByPolicyId(id);
                        // Add new capabilities
                        if (!request.getCapabilityIds().isEmpty()) {
                            assignCapabilities(id, request.getCapabilityIds());
                        }
                    }
                    
                    return ResponseEntity.ok(convertToResponse(policyRepository.findById(id).get()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete policy
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        if (policyRepository.existsById(id)) {
            // Delete policy capabilities first
            policyCapabilityRepository.deleteByPolicyId(id);
            // Delete policy
            policyRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Toggle policy active status
     */
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable Long id) {
        return policyRepository.findById(id)
                .map(policy -> {
                    policy.setIsActive(!policy.getIsActive());
                    Policy updated = policyRepository.save(policy);
                    return ResponseEntity.ok(convertToResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get capabilities assigned to a policy
     */
    @GetMapping("/{id}/capabilities")
    public ResponseEntity<List<Capability>> getPolicyCapabilities(@PathVariable Long id, HttpServletRequest request) {
        List<PolicyCapability> policyCapabilities = policyCapabilityRepository.findByPolicyId(id);
        List<Capability> capabilities = policyCapabilities.stream()
                .map(PolicyCapability::getCapability)
                .collect(Collectors.toList());
        try {
            String responseJson = objectMapper.writeValueAsString(capabilities);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(capabilities);
        } catch (Exception e) {
            logger.error("Error processing capabilities response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Assign capabilities to policy
     */
    @PostMapping("/{id}/capabilities")
    @Transactional
    public ResponseEntity<Map<String, Object>> assignCapabilitiesToPolicy(
            @PathVariable Long id,
            @RequestBody CapabilityAssignmentRequest request) {
        
        if (!policyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        assignCapabilities(id, request.getCapabilityIds());
        
        return ResponseEntity.ok(convertToResponse(policyRepository.findById(id).get()));
    }

    /**
     * Remove capability from policy
     */
    @DeleteMapping("/{id}/capabilities/{capabilityId}")
    @Transactional
    public ResponseEntity<Void> removeCapabilityFromPolicy(
            @PathVariable Long id,
            @PathVariable Long capabilityId) {
        
        policyCapabilityRepository.deleteByPolicyIdAndCapabilityId(id, capabilityId);
        return ResponseEntity.noContent().build();
    }

    // Helper methods
    
    private void assignCapabilities(Long policyId, Set<Long> capabilityIds) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found"));
        
        for (Long capabilityId : capabilityIds) {
            Capability capability = capabilityRepository.findById(capabilityId)
                    .orElseThrow(() -> new RuntimeException("Capability not found: " + capabilityId));
            
            // Check if already exists
            if (!policyCapabilityRepository.existsByPolicyIdAndCapabilityId(policyId, capabilityId)) {
                PolicyCapability pc = new PolicyCapability(policy, capability);
                policyCapabilityRepository.save(pc);
            }
        }
    }
    
    private Map<String, Object> convertToResponse(Policy policy) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", policy.getId());
        response.put("name", policy.getName());
        response.put("description", policy.getDescription());
        response.put("expression", policy.getExpression());
        response.put("isActive", policy.getIsActive());
        response.put("createdAt", policy.getCreatedAt());
        response.put("updatedAt", policy.getUpdatedAt());
        
        // Add capabilities
        Set<PolicyCapability> policyCapabilities = policy.getPolicyCapabilities();
        List<Map<String, Object>> capabilities = policyCapabilities.stream()
                .map(pc -> {
                    Map<String, Object> cap = new HashMap<>();
                    cap.put("id", pc.getCapability().getId());
                    cap.put("name", pc.getCapability().getName());
                    cap.put("description", pc.getCapability().getDescription());
                    return cap;
                })
                .collect(Collectors.toList());
        response.put("capabilities", capabilities);
        
        return response;
    }

    // DTO classes
    
    public static class PolicyRequest {
        private String name;
        private String description;
        private String type;
        private String expression;
        private Boolean isActive = true;
        private Set<Long> capabilityIds;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        
        public Set<Long> getCapabilityIds() { return capabilityIds; }
        public void setCapabilityIds(Set<Long> capabilityIds) { this.capabilityIds = capabilityIds; }
    }
    
    public static class CapabilityAssignmentRequest {
        private Set<Long> capabilityIds;

        public Set<Long> getCapabilityIds() { return capabilityIds; }
        public void setCapabilityIds(Set<Long> capabilityIds) { this.capabilityIds = capabilityIds; }
    }
}
