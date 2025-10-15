package com.example.userauth.controller;

import com.example.userauth.entity.Capability;
import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.PageAction;
import com.example.userauth.entity.UIPage;
import com.example.userauth.repository.CapabilityRepository;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.PageActionRepository;
import com.example.userauth.repository.UIPageRepository;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.shared.common.annotation.Auditable;

/**
 * Admin controller for managing page actions
 * Only accessible by ADMIN role
 */
@RestController
@RequestMapping("/api/admin/page-actions")
@SecurityRequirement(name = "Bearer Authentication")
public class PageActionController {

    private final PageActionRepository pageActionRepository;
    private final UIPageRepository uiPageRepository;
    private final CapabilityRepository capabilityRepository;
    private final EndpointRepository endpointRepository;

    public PageActionController(
            PageActionRepository pageActionRepository,
            UIPageRepository uiPageRepository,
            CapabilityRepository capabilityRepository,
            EndpointRepository endpointRepository) {
        this.pageActionRepository = pageActionRepository;
        this.uiPageRepository = uiPageRepository;
        this.capabilityRepository = capabilityRepository;
        this.endpointRepository = endpointRepository;
    }

    /**
     * Get all page actions
     */
    @Auditable(action = "GET_ALL_PAGE_ACTIONS", resourceType = "PAGE_ACTION")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllPageActions() {
        List<PageAction> actions = pageActionRepository.findAll();
        List<Map<String, Object>> response = actions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get page action by ID
     */
    @Auditable(action = "GET_PAGE_ACTION_BY_ID", resourceType = "PAGE_ACTION")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPageActionById(@PathVariable Long id) {
        return pageActionRepository.findById(id)
                .map(action -> ResponseEntity.ok(convertToResponse(action)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get actions for a specific page
     */
    @Auditable(action = "GET_ACTIONS_FOR_PAGE", resourceType = "PAGE_ACTION")
    @GetMapping("/page/{pageId}")
    public ResponseEntity<List<Map<String, Object>>> getActionsForPage(@PathVariable Long pageId) {
        List<PageAction> actions = pageActionRepository.findByPageIdAndIsActiveTrue(pageId);
        List<Map<String, Object>> response = actions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Create new page action
     */
    @Auditable(action = "CREATE_PAGE_ACTION", resourceType = "PAGE_ACTION")
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createPageAction(@RequestBody PageActionRequest request) {
        // Validate page
        UIPage page = uiPageRepository.findById(request.getPageId())
                .orElseThrow(() -> new RuntimeException("Page not found: " + request.getPageId()));
        
        // Validate capability
        Capability capability = capabilityRepository.findById(request.getCapabilityId())
                .orElseThrow(() -> new RuntimeException("Capability not found: " + request.getCapabilityId()));
        
        // Validate endpoint if provided
        Endpoint endpoint = null;
        if (request.getEndpointId() != null) {
            endpoint = endpointRepository.findById(request.getEndpointId())
                    .orElseThrow(() -> new RuntimeException("Endpoint not found: " + request.getEndpointId()));
        }
        
        PageAction action = new PageAction();
        action.setPage(page);
        action.setCapability(capability);
        action.setEndpoint(endpoint);
        action.setLabel(request.getLabel());
        action.setAction(request.getAction());
        action.setIcon(request.getIcon() != null ? request.getIcon() : "");
        action.setVariant(request.getVariant() != null ? request.getVariant() : "default");
        action.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        action.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        
        PageAction saved = pageActionRepository.save(action);
        return ResponseEntity.ok(convertToResponse(saved));
    }

    /**
     * Update page action
     */
    @Auditable(action = "UPDATE_PAGE_ACTION", resourceType = "PAGE_ACTION")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updatePageAction(
            @PathVariable Long id,
            @RequestBody PageActionRequest request) {
        
        return pageActionRepository.findById(id)
                .map(action -> {
                    // Validate page if changed
                    if (request.getPageId() != null && !request.getPageId().equals(action.getPage().getId())) {
                        UIPage page = uiPageRepository.findById(request.getPageId())
                                .orElseThrow(() -> new RuntimeException("Page not found: " + request.getPageId()));
                        action.setPage(page);
                    }
                    
                    // Validate capability if changed
                    if (request.getCapabilityId() != null && !request.getCapabilityId().equals(action.getCapability().getId())) {
                        Capability capability = capabilityRepository.findById(request.getCapabilityId())
                                .orElseThrow(() -> new RuntimeException("Capability not found: " + request.getCapabilityId()));
                        action.setCapability(capability);
                    }
                    
                    // Validate endpoint if changed
                    if (request.getEndpointId() != null) {
                        Endpoint endpoint = endpointRepository.findById(request.getEndpointId())
                                .orElseThrow(() -> new RuntimeException("Endpoint not found: " + request.getEndpointId()));
                        action.setEndpoint(endpoint);
                    } else {
                        action.setEndpoint(null);
                    }
                    
                    action.setLabel(request.getLabel());
                    action.setAction(request.getAction());
                    action.setIcon(request.getIcon() != null ? request.getIcon() : action.getIcon());
                    action.setVariant(request.getVariant() != null ? request.getVariant() : action.getVariant());
                    action.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : action.getDisplayOrder());
                    action.setIsActive(request.getIsActive() != null ? request.getIsActive() : action.getIsActive());
                    
                    PageAction updated = pageActionRepository.save(action);
                    return ResponseEntity.ok(convertToResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete page action
     */
    @Auditable(action = "DELETE_PAGE_ACTION", resourceType = "PAGE_ACTION")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deletePageAction(@PathVariable Long id) {
        if (pageActionRepository.existsById(id)) {
            pageActionRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Toggle page action active status
     */
    @Auditable(action = "TOGGLE_PAGE_ACTION_ACTIVE", resourceType = "PAGE_ACTION")
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable Long id) {
        return pageActionRepository.findById(id)
                .map(action -> {
                    action.setIsActive(!action.getIsActive());
                    PageAction updated = pageActionRepository.save(action);
                    return ResponseEntity.ok(convertToResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Reorder page action
     */
    @Auditable(action = "REORDER_PAGE_ACTION", resourceType = "PAGE_ACTION")
    @PatchMapping("/{id}/reorder")
    @Transactional
    public ResponseEntity<Map<String, Object>> reorderPageAction(
            @PathVariable Long id,
            @RequestBody ReorderRequest request) {
        
        return pageActionRepository.findById(id)
                .map(action -> {
                    action.setDisplayOrder(request.getNewDisplayOrder());
                    PageAction updated = pageActionRepository.save(action);
                    return ResponseEntity.ok(convertToResponse(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Helper methods
    
    private Map<String, Object> convertToResponse(PageAction action) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", action.getId());
        response.put("label", action.getLabel());
        response.put("action", action.getAction());
        response.put("icon", action.getIcon());
        response.put("variant", action.getVariant());
        response.put("displayOrder", action.getDisplayOrder());
        response.put("isActive", action.getIsActive());
        response.put("createdAt", action.getCreatedAt());
        response.put("updatedAt", action.getUpdatedAt());
        
        // Page info
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("id", action.getPage().getId());
        pageInfo.put("label", action.getPage().getLabel());
        pageInfo.put("route", action.getPage().getRoute());
        response.put("page", pageInfo);
        
        // Capability info
        Map<String, Object> capabilityInfo = new HashMap<>();
        capabilityInfo.put("id", action.getCapability().getId());
        capabilityInfo.put("name", action.getCapability().getName());
        capabilityInfo.put("description", action.getCapability().getDescription());
        response.put("capability", capabilityInfo);
        
        // Endpoint info (if exists)
        if (action.getEndpoint() != null) {
            Map<String, Object> endpointInfo = new HashMap<>();
            endpointInfo.put("id", action.getEndpoint().getId());
            endpointInfo.put("method", action.getEndpoint().getMethod());
            endpointInfo.put("path", action.getEndpoint().getPath());
            response.put("endpoint", endpointInfo);
        } else {
            response.put("endpoint", null);
        }
        
        return response;
    }

    // DTO classes
    
    public static class PageActionRequest {
        private Long pageId;
        private Long capabilityId;
        private Long endpointId;
        private String label;
        private String action;
        private String icon;
        private String variant;
        private Integer displayOrder;
        private Boolean isActive;

        // Getters and Setters
        public Long getPageId() { return pageId; }
        public void setPageId(Long pageId) { this.pageId = pageId; }
        
        public Long getCapabilityId() { return capabilityId; }
        public void setCapabilityId(Long capabilityId) { this.capabilityId = capabilityId; }
        
        public Long getEndpointId() { return endpointId; }
        public void setEndpointId(Long endpointId) { this.endpointId = endpointId; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        
        public String getVariant() { return variant; }
        public void setVariant(String variant) { this.variant = variant; }
        
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
    
    public static class ReorderRequest {
        private Integer newDisplayOrder;

        public Integer getNewDisplayOrder() { return newDisplayOrder; }
        public void setNewDisplayOrder(Integer newDisplayOrder) { this.newDisplayOrder = newDisplayOrder; }
    }
}
