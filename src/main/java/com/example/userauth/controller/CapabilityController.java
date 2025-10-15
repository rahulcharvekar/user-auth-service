package com.example.userauth.controller;

import com.example.userauth.entity.Capability;
import com.example.userauth.repository.CapabilityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import com.shared.common.util.ETagUtil;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import com.shared.common.annotation.Auditable;

/**
 * Admin controller for managing capabilities
 * Only accessible by ADMIN role
 */
@RestController
@RequestMapping("/api/admin/capabilities")
@SecurityRequirement(name = "Bearer Authentication")
public class CapabilityController {

    private static final Logger logger = LoggerFactory.getLogger(CapabilityController.class);

    private final CapabilityRepository capabilityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public CapabilityController(CapabilityRepository capabilityRepository) {
        this.capabilityRepository = capabilityRepository;
    }

    /**
     * Get all capabilities
     */
    @Auditable(action = "GET_ALL_CAPABILITIES", resourceType = "CAPABILITY")
    @GetMapping
    public ResponseEntity<List<Capability>> getAllCapabilities(HttpServletRequest request) {
        List<Capability> capabilities = capabilityRepository.findAll();
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
     * Get capability by ID
     */
    @Auditable(action = "GET_CAPABILITY_BY_ID", resourceType = "CAPABILITY")
        @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Capability> getCapabilityById(@PathVariable Long id, HttpServletRequest request) {
        return capabilityRepository.findById(id)
                .map(capability -> {
                    try {
                        String responseJson = objectMapper.writeValueAsString(capability);
                        String eTag = ETagUtil.generateETag(responseJson);
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (eTag.equals(ifNoneMatch)) {
                            return (ResponseEntity<Capability>) (ResponseEntity<?>) ResponseEntity.status(304).eTag(eTag).build();
                        }
                        return (ResponseEntity<Capability>) (ResponseEntity<?>) ResponseEntity.ok().eTag(eTag).body(capability);
                    } catch (Exception e) {
                        logger.error("Error processing capability response", e);
                        return (ResponseEntity<Capability>) (ResponseEntity<?>) ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new capability
     */
    @Auditable(action = "CREATE_CAPABILITY", resourceType = "CAPABILITY")
    @PostMapping
    public ResponseEntity<Capability> createCapability(@RequestBody CapabilityRequest request) {
        Capability capability = new Capability(
                request.getName(),
                request.getDescription(),
                request.getModule(),
                request.getAction(),
                request.getResource()
        );
        Capability saved = capabilityRepository.save(capability);
        return ResponseEntity.ok(saved);
    }

    /**
     * Update capability
     */
    @Auditable(action = "UPDATE_CAPABILITY", resourceType = "CAPABILITY")
    @PutMapping("/{id}")
    public ResponseEntity<Capability> updateCapability(
            @PathVariable Long id,
            @RequestBody CapabilityRequest request) {
        
        return capabilityRepository.findById(id)
                .map(capability -> {
                    capability.setName(request.getName());
                    capability.setDescription(request.getDescription());
                    capability.setModule(request.getModule());
                    capability.setAction(request.getAction());
                    capability.setResource(request.getResource());
                    capability.setIsActive(request.getIsActive());
                    Capability updated = capabilityRepository.save(capability);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete capability
     */
    @Auditable(action = "DELETE_CAPABILITY", resourceType = "CAPABILITY")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCapability(@PathVariable Long id) {
        if (capabilityRepository.existsById(id)) {
            capabilityRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Activate/Deactivate capability
     */
    @Auditable(action = "TOGGLE_CAPABILITY_ACTIVE", resourceType = "CAPABILITY")
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<Capability> toggleActive(@PathVariable Long id) {
        return capabilityRepository.findById(id)
                .map(capability -> {
                    capability.setIsActive(!capability.getIsActive());
                    Capability updated = capabilityRepository.save(capability);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DTO classes
    public static class CapabilityRequest {
        private String name;
        private String description;
        private String module;
        private String action;
        private String resource;
        private Boolean isActive = true;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getModule() { return module; }
        public void setModule(String module) { this.module = module; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
}
