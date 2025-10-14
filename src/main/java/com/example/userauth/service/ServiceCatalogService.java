package com.example.userauth.service;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.UIPage;
import com.example.userauth.entity.Policy;
import com.example.userauth.entity.EndpointPolicy;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.UIPageRepository;
import com.example.userauth.repository.PolicyRepository;
import com.example.userauth.repository.EndpointPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Catalog Service - Provides metadata about available endpoints and UI pages
 * Frontend applications can use this to discover available services
 */
@Service
public class ServiceCatalogService {
    /**
     * Get all endpoints for a given UI page id using capability-policy-endpoint linkage
     *
     * @param pageId the UI page id
     * @return List of endpoints for the given page id
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEndpointsByPageId(Long pageId) {
        // 1. Get requiredCapability from UIPage
        Optional<UIPage> pageOpt = uiPageRepository.findById(pageId);
        if (pageOpt.isEmpty()) return List.of();
        String capability = pageOpt.get().getRequiredCapability();
        if (capability == null) return List.of();

        // 2. Find all policies that grant this capability
        List<Policy> policies = policyRepository.findPoliciesByCapabilityName(capability);
        if (policies.isEmpty()) return List.of();
        List<Long> policyIds = policies.stream().map(Policy::getId).toList();

        // 3. Find all endpoint-policy associations for these policies
        List<EndpointPolicy> endpointPolicies = endpointPolicyRepository.findByPolicyIdIn(policyIds);
        if (endpointPolicies.isEmpty()) return List.of();
        List<Long> endpointIds = endpointPolicies.stream().map(ep -> ep.getEndpoint().getId()).toList();

        // 4. Fetch endpoints by these ids
    List<Endpoint> endpoints = endpointRepository.findByIdIn(endpointIds);
        return endpoints.stream().map(this::mapEndpointToDto).toList();
    }
    /**
     * Get all endpoints for a given parent_id by traversing all lineage (descendants)
     *
     * @param parentId the parent ID to filter endpoints by lineage
     * @return List of endpoints for the given parent_id lineage
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEndpointsByParentLineage(Long parentId) {
        // This assumes Endpoint has a parentId field, which it currently does not.
        // If Endpoint is not hierarchical, this will just return all endpoints (or none).
        // Placeholder for actual implementation if hierarchy is added later.
        // For now, return all endpoints (or filter by parentId if such a field is added).
        // TODO: Implement actual lineage traversal if Endpoint supports hierarchy.
        List<Endpoint> endpoints = endpointRepository.findByIsActiveTrue();
        // If Endpoint had getParentId(), you would filter and traverse here.
        // Returning all for now.
        return endpoints.stream()
                .map(this::mapEndpointToDto)
                .collect(Collectors.toList());
    }

    private static final Logger logger = LoggerFactory.getLogger(ServiceCatalogService.class);

    private final EndpointRepository endpointRepository;
    private final UIPageRepository uiPageRepository;
    private final PolicyRepository policyRepository;
    private final EndpointPolicyRepository endpointPolicyRepository;

    public ServiceCatalogService(EndpointRepository endpointRepository, UIPageRepository uiPageRepository, PolicyRepository policyRepository, EndpointPolicyRepository endpointPolicyRepository) {
        this.endpointRepository = endpointRepository;
        this.uiPageRepository = uiPageRepository;
        this.policyRepository = policyRepository;
        this.endpointPolicyRepository = endpointPolicyRepository;
    }
    // ...existing code...
    // Helper method for endpoint policies by multiple policy ids
    // (Assumes you have a method in EndpointPolicyRepository: List<EndpointPolicy> findByPolicyIdIn(List<Long> policyIds))

    /**
     * Get complete service catalog (all endpoints and pages)
     * 
     * @return Map containing endpoints and pages
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getServiceCatalog() {
        logger.debug("Building service catalog");

        Map<String, Object> catalog = new HashMap<>();
        catalog.put("endpoints", getEndpointsCatalog());
        catalog.put("pages", getPagesCatalog());
        catalog.put("version", System.currentTimeMillis());

        return catalog;
    }

    /**
     * Get all active endpoints grouped by module
     * 
     * @return Map of module -> list of endpoints
     */
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> getEndpointsCatalog() {
        List<Endpoint> endpoints = endpointRepository.findByIsActiveTrue();

        Map<String, List<Map<String, Object>>> endpointsByService = new HashMap<>();

        for (Endpoint endpoint : endpoints) {
            Map<String, Object> endpointData = new HashMap<>();
            endpointData.put("service", endpoint.getService());
            endpointData.put("version", endpoint.getVersion());
            endpointData.put("method", endpoint.getMethod());
            endpointData.put("path", endpoint.getPath());
            endpointData.put("description", endpoint.getDescription());
            endpointData.put("ui_type", endpoint.getUiType());

            endpointsByService
                    .computeIfAbsent(endpoint.getService(), k -> new ArrayList<>())
                    .add(endpointData);
        }

        logger.debug("Cataloged {} endpoints across {} services", endpoints.size(), endpointsByService.size());
        return endpointsByService;
    }

    /**
     * Get all active pages in hierarchical structure
     * 
     * @return Hierarchical list of pages
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPagesCatalog() {
        List<UIPage> pages = uiPageRepository.findByIsActiveTrue();

        List<Map<String, Object>> pageList = pages.stream()
                .map(this::mapPageToDto)
                .collect(Collectors.toList());

        logger.debug("Cataloged {} pages", pages.size());
        return buildPageHierarchy(pageList);
    }

    /**
     * Get all endpoints for a specific service
     * 
     * @param service The service name
     * @return List of endpoints
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEndpointsByService(String service) {
        List<Endpoint> endpoints = endpointRepository.findByServiceAndIsActiveTrue(service);

        return endpoints.stream()
                .map(this::mapEndpointToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all pages for a specific module
     * 
     * @param module The module name
     * @return List of pages
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPagesByModule(String module) {
        List<UIPage> pages = uiPageRepository.findByModule(module);

        return pages.stream()
                .map(this::mapPageToDto)
                .collect(Collectors.toList());
    }

    /**
     * Map Endpoint entity to DTO with enhanced metadata from annotations
     */
    private Map<String, Object> mapEndpointToDto(Endpoint endpoint) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("service", endpoint.getService());
        dto.put("version", endpoint.getVersion());
        dto.put("method", endpoint.getMethod());
        dto.put("path", endpoint.getPath());
        dto.put("description", endpoint.getDescription());
        dto.put("ui_type", endpoint.getUiType());

        // Try to enhance with annotation data if available
        Map<String, Object> annotationData = getAnnotationDataForEndpoint(endpoint);
        if (annotationData != null) {
            dto.putAll(annotationData);
        }

        return dto;
    }

    /**
     * Get annotation data for an endpoint by scanning controller methods
     */
    private Map<String, Object> getAnnotationDataForEndpoint(Endpoint endpoint) {
        try {
            // This is a simplified approach - in production you might want to cache this
            // or store it in the database during application startup
            String fullPath = "/api/" + endpoint.getService() + "/" + endpoint.getPath().substring(1);
            return scanForUiTypeAnnotation(fullPath, endpoint.getMethod());
        } catch (Exception e) {
            // If annotation scanning fails, just return null
            return null;
        }
    }

    /**
     * Scan for UiType annotation on controller methods
     */
    private Map<String, Object> scanForUiTypeAnnotation(String path, String method) {
        // This is a placeholder - you'd need to implement classpath scanning
        // For now, return null and rely on database uiType field
        return null;
    }

    /**
     * Map UIPage entity to DTO
     */
    private Map<String, Object> mapPageToDto(UIPage page) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", page.getId());
        dto.put("key", page.getKey());
        dto.put("label", page.getLabel());
        dto.put("route", page.getRoute());
        dto.put("icon", page.getIcon());
        dto.put("module", page.getModule());
        dto.put("parentId", page.getParentId());
        dto.put("displayOrder", page.getDisplayOrder());
        dto.put("isMenuItem", page.getIsMenuItem());
        return dto;
    }

    /**
     * Build hierarchical page structure
     */
    private List<Map<String, Object>> buildPageHierarchy(List<Map<String, Object>> pages) {
        // Separate root and child pages
        List<Map<String, Object>> rootPages = new ArrayList<>();
        Map<Long, List<Map<String, Object>>> childrenMap = new HashMap<>();

        for (Map<String, Object> page : pages) {
            Long parentId = (Long) page.get("parentId");
            if (parentId == null) {
                rootPages.add(page);
            } else {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(page);
            }
        }

        // Attach children to parents recursively
        attachChildren(rootPages, childrenMap);

        return rootPages;
    }

    /**
     * Recursively attach children to parent pages
     */
    private void attachChildren(List<Map<String, Object>> pages, Map<Long, List<Map<String, Object>>> childrenMap) {
        for (Map<String, Object> page : pages) {
            Long pageId = (Long) page.get("id");
            List<Map<String, Object>> children = childrenMap.get(pageId);
            if (children != null && !children.isEmpty()) {
                page.put("children", children);
                attachChildren(children, childrenMap);
            }
        }
    }
}
