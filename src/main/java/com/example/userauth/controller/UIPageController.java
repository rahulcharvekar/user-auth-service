package com.example.userauth.controller;

import com.example.userauth.audit.annotation.Audited;
import com.example.userauth.entity.UIPage;
import com.example.userauth.repository.UIPageRepository;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.example.userauth.repository.PageActionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Admin controller for managing UI pages
 * Only accessible by ADMIN role
 */
@RestController
@RequestMapping("/api/admin/ui-pages")
@SecurityRequirement(name = "Bearer Authentication")
public class UIPageController {

    private static final Logger logger = LoggerFactory.getLogger(UIPageController.class);

    @Autowired
    private ObjectMapper objectMapper;

    private final UIPageRepository uiPageRepository;
    private final PageActionRepository pageActionRepository;

    public UIPageController(
            UIPageRepository uiPageRepository,
            PageActionRepository pageActionRepository) {
        this.uiPageRepository = uiPageRepository;
        this.pageActionRepository = pageActionRepository;
    }

    /**
     * Get all pages with hierarchy
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPages() {
        List<UIPage> pages = uiPageRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        
        // Convert to response format
        List<Map<String, Object>> pageList = pages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        // Build hierarchy
        Map<String, Object> response = new HashMap<>();
        response.put("pages", pageList);
        response.put("tree", buildTree(pageList));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all pages (including inactive)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllPagesIncludingInactive() {
        List<UIPage> pages = uiPageRepository.findAll();
        List<Map<String, Object>> response = pages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get page by ID
     */
    @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> getPageById(@PathVariable Long id) {
        return uiPageRepository.findById(id)
                .map(page -> {
                    Map<String, Object> response = convertToResponse(page);
                    try {
                        objectMapper.writeValueAsString(response);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.ok(response);
                    } catch (Exception e) {
                        logger.error("Error processing page response", e);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new page
     */
    @PostMapping
    @Transactional
    @Audited(action = "CREATE_UI_PAGE", resourceType = "UI_PAGE")
    public ResponseEntity<Map<String, Object>> createPage(@RequestBody PageRequest request) {
        // Validate parent if provided
        if (request.getParentId() != null && request.getParentId() != 0) {
            if (!uiPageRepository.existsById(request.getParentId())) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        // Generate key from route if not provided
        String key = request.getRoute().startsWith("/") ? 
            request.getRoute().substring(1).replace("/", ".") : 
            request.getRoute().replace("/", ".");
        
        UIPage page = new UIPage(key, request.getLabel(), request.getRoute(), "default");
        page.setIcon(request.getIcon());
        page.setParentId(request.getParentId() != null && request.getParentId() != 0 ? request.getParentId() : null);
        page.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        page.setIsMenuItem(request.getIsMenuItem() != null ? request.getIsMenuItem() : true);
        page.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        
        UIPage saved = uiPageRepository.save(page);
        Map<String, Object> response = convertToResponse(saved);
        try {
            objectMapper.writeValueAsString(response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing page creation response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update page
     */
    @PutMapping("/{id}")
    @Transactional
    @Audited(action = "UPDATE_UI_PAGE", resourceType = "UI_PAGE")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> updatePage(
            @PathVariable Long id,
            @RequestBody PageRequest request) {
        
        return uiPageRepository.findById(id)
                .map(page -> {
                    // Validate parent if provided
                    if (request.getParentId() != null && request.getParentId() != 0) {
                        if (request.getParentId().equals(id)) {
                            throw new RuntimeException("Page cannot be its own parent");
                        }
                        if (!uiPageRepository.existsById(request.getParentId())) {
                            throw new RuntimeException("Parent page not found");
                        }
                    }
                    
                    page.setLabel(request.getLabel());
                    page.setRoute(request.getRoute());
                    page.setIcon(request.getIcon());
                    page.setParentId(request.getParentId() != null && request.getParentId() != 0 ? request.getParentId() : null);
                    page.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : page.getDisplayOrder());
                    page.setIsMenuItem(request.getIsMenuItem() != null ? request.getIsMenuItem() : page.getIsMenuItem());
                    page.setIsActive(request.getIsActive() != null ? request.getIsActive() : page.getIsActive());
                    
                    UIPage updated = uiPageRepository.save(page);
                    Map<String, Object> response = convertToResponse(updated);
                    try {
                        objectMapper.writeValueAsString(response);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.ok(response);
                    } catch (Exception e) {
                        logger.error("Error processing page update response", e);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete page
     */
    @DeleteMapping("/{id}")
    @Transactional
    @Audited(action = "DELETE_UI_PAGE", resourceType = "UI_PAGE")
    public ResponseEntity<Void> deletePage(@PathVariable Long id) {
        if (uiPageRepository.existsById(id)) {
            // Check if page has children
            List<UIPage> children = uiPageRepository.findByParentId(id);
            if (!children.isEmpty()) {
                throw new RuntimeException("Cannot delete page with children. Delete children first.");
            }
            
            // Delete page actions first
            pageActionRepository.deleteByPageId(id);
            
            // Delete page
            uiPageRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Toggle page active status
     */
    @PatchMapping("/{id}/toggle-active")
    @Audited(action = "TOGGLE_UI_PAGE_ACTIVE", resourceType = "UI_PAGE")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable Long id) {
        return uiPageRepository.findById(id)
                .map(page -> {
                    page.setIsActive(!page.getIsActive());
                    UIPage updated = uiPageRepository.save(page);
                    Map<String, Object> response = convertToResponse(updated);
                    try {
                        objectMapper.writeValueAsString(response);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.ok(response);
                    } catch (Exception e) {
                        logger.error("Error processing page toggle response", e);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Reorder page
     */
    @PatchMapping("/{id}/reorder")
    @Transactional
    @Audited(action = "REORDER_UI_PAGE", resourceType = "UI_PAGE")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> reorderPage(
            @PathVariable Long id,
            @RequestBody ReorderRequest request) {
        
        return uiPageRepository.findById(id)
                .map(page -> {
                    page.setDisplayOrder(request.getNewDisplayOrder());
                    UIPage updated = uiPageRepository.save(page);
                    Map<String, Object> response = convertToResponse(updated);
                    try {
                        objectMapper.writeValueAsString(response);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.ok(response);
                    } catch (Exception e) {
                        logger.error("Error processing page reorder response", e);
                        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get child pages
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<List<Map<String, Object>>> getChildPages(@PathVariable Long id) {
        List<UIPage> children = uiPageRepository.findByParentId(id);
        List<Map<String, Object>> response = children.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        try {
            objectMapper.writeValueAsString(response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing child pages response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods
    
    private Map<String, Object> convertToResponse(UIPage page) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", page.getId());
        response.put("label", page.getLabel());
        response.put("route", page.getRoute());
        response.put("icon", page.getIcon());
        response.put("parentId", page.getParentId());
        response.put("displayOrder", page.getDisplayOrder());
        response.put("isMenuItem", page.getIsMenuItem());
        response.put("isActive", page.getIsActive());
        response.put("createdAt", page.getCreatedAt());
        response.put("updatedAt", page.getUpdatedAt());
        
        // Count actions
        long actionCount = pageActionRepository.countByPageId(page.getId());
        response.put("actionCount", actionCount);
        
        return response;
    }
    
    private List<Map<String, Object>> buildTree(List<Map<String, Object>> pages) {
        Map<Long, Map<String, Object>> pageMap = new HashMap<>();
        List<Map<String, Object>> rootPages = new ArrayList<>();
        
        // First pass: create lookup map
        for (Map<String, Object> page : pages) {
            pageMap.put((Long) page.get("id"), new HashMap<>(page));
        }
        
        // Second pass: build tree
        for (Map<String, Object> page : pages) {
            Map<String, Object> pageNode = pageMap.get(page.get("id"));
            Long parentId = (Long) page.get("parentId");
            
            if (parentId == null) {
                rootPages.add(pageNode);
            } else {
                Map<String, Object> parent = pageMap.get(parentId);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) 
                        parent.computeIfAbsent("children", k -> new ArrayList<>());
                    children.add(pageNode);
                }
            }
        }
        
        return rootPages;
    }

    // DTO classes
    
    public static class PageRequest {
        private String label;
        private String route;
        private String icon;
        private Long parentId;
        private Integer displayOrder;
        private Boolean isMenuItem;
        private Boolean isActive;

        // Getters and Setters
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        
        public String getRoute() { return route; }
        public void setRoute(String route) { this.route = route; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
        
        public Boolean getIsMenuItem() { return isMenuItem; }
        public void setIsMenuItem(Boolean isMenuItem) { this.isMenuItem = isMenuItem; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
    
    public static class ReorderRequest {
        private Integer newDisplayOrder;

        public Integer getNewDisplayOrder() { return newDisplayOrder; }
        public void setNewDisplayOrder(Integer newDisplayOrder) { this.newDisplayOrder = newDisplayOrder; }
    }
}
