package com.example.userauth.controller;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.EndpointPolicy;
import com.example.userauth.entity.Policy;
import com.example.userauth.repository.EndpointPolicyRepository;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.PolicyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import com.shared.common.util.ETagUtil;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.shared.common.annotation.Auditable;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Admin controller for managing endpoints and their policy assignments
 * Only accessible by ADMIN role
 */
@RestController
@RequestMapping("/api/admin/endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class EndpointController {

    private static final Logger logger = LoggerFactory.getLogger(EndpointController.class);

    @Autowired
    private ObjectMapper objectMapper;

    private final EndpointRepository endpointRepository;
    private final PolicyRepository policyRepository;
    private final EndpointPolicyRepository endpointPolicyRepository;

    public EndpointController(
            EndpointRepository endpointRepository,
            PolicyRepository policyRepository,
            EndpointPolicyRepository endpointPolicyRepository) {
        this.endpointRepository = endpointRepository;
        this.policyRepository = policyRepository;
        this.endpointPolicyRepository = endpointPolicyRepository;
    }

        /**
     * Get all endpoints with their policies
     */
    @Auditable(action = "GET_ALL_ENDPOINTS", resourceType = "ENDPOINT")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllEndpoints(HttpServletRequest request) {
        List<Endpoint> endpoints = endpointRepository.findAll();
        List<Map<String, Object>> response = endpoints.stream()
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
            logger.error("Error processing endpoints response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get endpoint by ID with policies
     */
    @Auditable(action = "GET_ENDPOINT_BY_ID", resourceType = "ENDPOINT")
    @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getEndpointById(@PathVariable Long id, HttpServletRequest request) {
        return endpointRepository.findById(id)
                .map(endpoint -> {
                    Map<String, Object> response = convertToResponse(endpoint);
                    try {
                        String responseJson = objectMapper.writeValueAsString(response);
                        String eTag = ETagUtil.generateETag(responseJson);
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (eTag.equals(ifNoneMatch)) {
                            return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.status(304).eTag(eTag).build();
                        }
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.ok().eTag(eTag).body(response);
                    } catch (Exception e) {
                        logger.error("Error processing endpoint response", e);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new endpoint
     */
    @PostMapping
    @Transactional
    @Auditable(action = "CREATE_ENDPOINT", resourceType = "ENDPOINT")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createEndpoint(@RequestBody EndpointRequest request) {
        Endpoint endpoint = new Endpoint(
                request.getService(),
                request.getVersion(),
                request.getMethod(),
                request.getPath(),
                request.getDescription()
        );
        endpoint.setIsActive(request.getIsActive());
        Endpoint saved = endpointRepository.save(endpoint);
        
        // Assign policies if provided
        if (request.getPolicyIds() != null && !request.getPolicyIds().isEmpty()) {
            assignPolicies(saved.getId(), request.getPolicyIds());
        }
        
        return ResponseEntity.ok(convertToResponse(endpointRepository.findById(saved.getId()).get()));
    }

    /**
     * Update endpoint
     */
    @PutMapping("/{id}")
    @Transactional
    @Auditable(action = "UPDATE_ENDPOINT", resourceType = "ENDPOINT")
    public ResponseEntity<Map<String, Object>> updateEndpoint(
            @PathVariable Long id,
            @RequestBody EndpointRequest request) {
        
        return endpointRepository.findById(id)
                .map(endpoint -> {
                    endpoint.setService(request.getService());
                    endpoint.setVersion(request.getVersion());
                    endpoint.setMethod(request.getMethod());
                    endpoint.setPath(request.getPath());
                    endpoint.setDescription(request.getDescription());
                    endpoint.setIsActive(request.getIsActive());
                    endpointRepository.save(endpoint);
                    
                    // Update policies if provided
                    if (request.getPolicyIds() != null) {
                        // Remove existing policies
                        endpointPolicyRepository.deleteByEndpointId(id);
                        // Add new policies
                        if (!request.getPolicyIds().isEmpty()) {
                            assignPolicies(id, request.getPolicyIds());
                        }
                    }
                    
                    return ResponseEntity.ok(convertToResponse(endpointRepository.findById(id).get()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete endpoint
     */
    @DeleteMapping("/{id}")
    @Transactional
    @Auditable(action = "DELETE_ENDPOINT", resourceType = "ENDPOINT")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable Long id) {
        if (endpointRepository.existsById(id)) {
            // Delete endpoint policies first
            endpointPolicyRepository.deleteByEndpointId(id);
            // Delete endpoint
            endpointRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Toggle endpoint active status
     */
    @PatchMapping("/{id}/toggle-active")
    @Auditable(action = "TOGGLE_ENDPOINT_ACTIVE", resourceType = "ENDPOINT")
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable Long id) {
        return endpointRepository.findById(id)
                .map(endpoint -> {
                    endpoint.setIsActive(!endpoint.getIsActive());
                    Endpoint updated = endpointRepository.save(endpoint);
                    return ResponseEntity.ok(convertToResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get policies assigned to an endpoint
     */
    @GetMapping("/{id}/policies")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Policy>> getEndpointPolicies(@PathVariable Long id, HttpServletRequest request) {
        List<EndpointPolicy> endpointPolicies = endpointPolicyRepository.findByEndpointId(id);
        List<Policy> policies = endpointPolicies.stream()
                .map(EndpointPolicy::getPolicy)
                .collect(Collectors.toList());
        try {
            String responseJson = objectMapper.writeValueAsString(policies);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(policies);
        } catch (Exception e) {
            logger.error("Error processing policies response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Assign policies to endpoint
     */
    @PostMapping("/{id}/policies")
    @Transactional
    @Auditable(action = "ASSIGN_POLICIES_TO_ENDPOINT", resourceType = "ENDPOINT")
    public ResponseEntity<Map<String, Object>> assignPoliciesToEndpoint(
            @PathVariable Long id,
            @RequestBody PolicyAssignmentRequest request) {
        
        if (!endpointRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        assignPolicies(id, request.getPolicyIds());
        
        return ResponseEntity.ok(convertToResponse(endpointRepository.findById(id).get()));
    }

    /**
     * Remove policy from endpoint
     */
    @DeleteMapping("/{id}/policies/{policyId}")
    @Transactional
    @Auditable(action = "REMOVE_POLICY_FROM_ENDPOINT", resourceType = "ENDPOINT")
    public ResponseEntity<Void> removePolicyFromEndpoint(
            @PathVariable Long id,
            @PathVariable Long policyId) {
        
        endpointPolicyRepository.deleteByEndpointIdAndPolicyId(id, policyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk assign a single policy to multiple endpoints
     */
    @PostMapping("/bulk-policy-assignment")
    @Transactional
    @Auditable(action = "BULK_ASSIGN_POLICY_TO_ENDPOINTS", resourceType = "ENDPOINT")
    public ResponseEntity<Map<String, Object>> bulkAssignPolicyToEndpoints(
            @RequestBody BulkPolicyAssignmentRequest request) {

        if (request.getPolicyId() == null) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (request.getEndpointIds() == null || request.getEndpointIds().isEmpty()) {
            throw new IllegalArgumentException("At least one endpointId is required");
        }

        Long policyId = request.getPolicyId();
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        long nextId = endpointPolicyRepository.findTopByOrderByIdDesc()
                .map(existing -> existing.getId() + 1)
                .orElse(1L);

        List<Long> newlyAssigned = new ArrayList<>();

        for (Long endpointId : request.getEndpointIds()) {
            Endpoint endpoint = endpointRepository.findById(endpointId)
                    .orElseThrow(() -> new IllegalArgumentException("Endpoint not found: " + endpointId));

            if (!endpointPolicyRepository.existsByEndpointIdAndPolicyId(endpointId, policyId)) {
                EndpointPolicy ep = new EndpointPolicy(endpoint, policy);
                ep.setId(nextId++);
                endpointPolicyRepository.save(ep);
                newlyAssigned.add(endpointId);
            }
        }

        List<Map<String, Object>> endpointSummaries = endpointPolicyRepository.findByPolicyId(policyId)
                .stream()
                .map(ep -> {
                    Endpoint endpoint = ep.getEndpoint();
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("id", endpoint.getId());
                    summary.put("service", endpoint.getService());
                    summary.put("version", endpoint.getVersion());
                    summary.put("method", endpoint.getMethod());
                    summary.put("path", endpoint.getPath());
                    summary.put("description", endpoint.getDescription());
                    return summary;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("policyId", policyId);
        response.put("newlyAssignedEndpointIds", newlyAssigned);
        response.put("totalEndpointCount", endpointSummaries.size());
        response.put("endpoints", endpointSummaries);

        return ResponseEntity.ok(response);
    }

    // Helper methods
    
    private void assignPolicies(Long endpointId, Set<Long> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return;
        }
        Endpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint not found"));
        
        long nextId = endpointPolicyRepository.findTopByOrderByIdDesc()
                .map(existing -> existing.getId() + 1)
                .orElse(1L);
        
        for (Long policyId : policyIds) {
            Policy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found: " + policyId));
            
            // Check if already exists
            if (!endpointPolicyRepository.existsByEndpointIdAndPolicyId(endpointId, policyId)) {
                EndpointPolicy ep = new EndpointPolicy(endpoint, policy);
                ep.setId(nextId++);
                endpointPolicyRepository.save(ep);
            }
        }
    }
    
    private Map<String, Object> convertToResponse(Endpoint endpoint) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", endpoint.getId());
        response.put("service", endpoint.getService());
        response.put("version", endpoint.getVersion());
        response.put("method", endpoint.getMethod());
        response.put("path", endpoint.getPath());
        response.put("description", endpoint.getDescription());
        response.put("isActive", endpoint.getIsActive());
        response.put("createdAt", endpoint.getCreatedAt());
        response.put("updatedAt", endpoint.getUpdatedAt());
        
        // Add policies
        Set<EndpointPolicy> endpointPolicies = endpoint.getEndpointPolicies();
        List<Map<String, Object>> policies = endpointPolicies.stream()
                .map(ep -> {
                    Map<String, Object> pol = new HashMap<>();
                    pol.put("id", ep.getPolicy().getId());
                    pol.put("name", ep.getPolicy().getName());
                    pol.put("description", ep.getPolicy().getDescription());
                    return pol;
                })
                .collect(Collectors.toList());
        response.put("policies", policies);
        
        return response;
    }

    // DTO classes
    
    public static class EndpointRequest {
        private String service;
        private String version;
        private String method;
        private String path;
        private String description;
        private Boolean isActive = true;
        private Set<Long> policyIds;

        // Getters and Setters
        public String getService() { return service; }
        public void setService(String service) { this.service = service; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        
        public Set<Long> getPolicyIds() { return policyIds; }
        public void setPolicyIds(Set<Long> policyIds) { this.policyIds = policyIds; }
    }
    
    public static class PolicyAssignmentRequest {
        private Set<Long> policyIds;

        public Set<Long> getPolicyIds() { return policyIds; }
        public void setPolicyIds(Set<Long> policyIds) { this.policyIds = policyIds; }
    }

    public static class BulkPolicyAssignmentRequest {
        private Long policyId;
        private Set<Long> endpointIds;

        public Long getPolicyId() {
            return policyId;
        }

        public void setPolicyId(Long policyId) {
            this.policyId = policyId;
        }

        public Set<Long> getEndpointIds() {
            return endpointIds;
        }

        public void setEndpointIds(Set<Long> endpointIds) {
            this.endpointIds = endpointIds;
        }
    }
}
