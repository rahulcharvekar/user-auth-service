package com.example.userauth.service;

import com.example.userauth.entity.*;
import com.example.userauth.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Authorization Service - Unified authorization flow implementation
 * 
 * ARCHITECTURE:
 * User → UserRoleAssignment → Role → Policy → {Capability, Endpoint}
 * PageAction → Capability ↑ (linked via PolicyCapability)
 * 
 * PRINCIPLE: Policy is the single source of truth
 * - If Policy grants Capability, it MUST grant required Endpoints
 * - User access is determined by: Role → Policy → {Capabilities + Endpoints}
 * 
 * Returns capabilities, policies, endpoints, and UI pages for authenticated users
 */
@Service
public class AuthorizationService {
    /**
     * Get all endpoints for a given UI page id (regardless of user)
     *
     * @param pageId the UI page id
     * @return List of endpoint details for the page
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEndpointsForPage(Long pageId) {
        List<PageAction> actions = pageActionRepository.findByPageIdAndIsActiveTrue(pageId);
        List<Map<String, Object>> endpoints = new ArrayList<>();
        for (PageAction action : actions) {
            if (action.getEndpoint() != null) {
                Map<String, Object> endpointData = new HashMap<>();
                endpointData.put("method", action.getEndpoint().getMethod());
                endpointData.put("path", action.getEndpoint().getPath());
                endpointData.put("service", action.getEndpoint().getService());
                endpointData.put("version", action.getEndpoint().getVersion());
                endpointData.put("description", action.getEndpoint().getDescription());
                endpointData.put("ui_type", action.getEndpoint().getUiType());
                endpoints.add(endpointData);
            }
        }
        return endpoints;
    }

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository userRoleRepository;
    private final CapabilityRepository capabilityRepository;
    private final UIPageRepository uiPageRepository;
    private final PageActionRepository pageActionRepository;

    public AuthorizationService(
            UserRepository userRepository,
            UserRoleAssignmentRepository userRoleRepository,
            PolicyRepository policyRepository,
            CapabilityRepository capabilityRepository,
            EndpointRepository endpointRepository,
            UIPageRepository uiPageRepository,
            PageActionRepository pageActionRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.capabilityRepository = capabilityRepository;
        this.uiPageRepository = uiPageRepository;
        this.pageActionRepository = pageActionRepository;
    }

    /**
     * Get comprehensive authorization data for a user
     * 
     * @param userId The user ID
     * @return Map containing roles, capabilities, pages, and menu tree
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserAuthorizations(Long userId) {
        logger.debug("Building authorization response for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Get user's roles
        List<UserRoleAssignment> userRoles = userRoleRepository.findByUserId(userId);
        Set<String> roleNames = userRoles.stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());

        logger.debug("User {} has roles: {}", userId, roleNames);

    // Get capabilities for user's roles
    Set<String> capabilities = getCapabilitiesForRoles(roleNames);

    // Get accessible pages for user's roles, filtered by capabilities
    List<Map<String, Object>> pages = getAccessiblePagesFilteredByCapabilities(roleNames, capabilities);

    // Build response (without endpoints)
    Map<String, Object> response = new HashMap<>();
    response.put("userId", userId);
    response.put("username", user.getUsername());
    response.put("roles", roleNames);
    response.put("can", buildCapabilityMap(capabilities)); // { "USER_CREATE": true, ...}
    response.put("pages", pages);
    response.put("version", System.currentTimeMillis()); // For client-side cache invalidation

    logger.debug("Authorization response built successfully for user: {}", userId);
    return response;
    }

    /**
     * Get all capabilities granted to specific roles
     */
    private Set<String> getCapabilitiesForRoles(Set<String> roleNames) {
        Set<String> capabilities = new HashSet<>();

        for (String roleName : roleNames) {
            List<String> roleCapabilities = capabilityRepository.findCapabilityNamesByRoleName(roleName);
            capabilities.addAll(roleCapabilities);
        }

        logger.debug("Found {} capabilities for roles: {}", capabilities.size(), roleNames);
        return capabilities;
    }

    /**
     * Get accessible pages for user's roles
     */
    private List<Map<String, Object>> getAccessiblePagesFilteredByCapabilities(Set<String> roleNames, Set<String> capabilities) {
        List<UIPage> allPages = uiPageRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<Map<String, Object>> accessiblePages = new ArrayList<>();
        Set<Long> accessiblePageIds = new HashSet<>();

        // First pass: collect pages with actions
        for (UIPage page : allPages) {
            List<PageAction> actions = pageActionRepository.findByPageIdAndIsActiveTrue(page.getId());
            // Only include actions the user can perform (has capability)
            List<Map<String, Object>> userActions = actions.stream()
                .filter(action -> action.getCapability() != null && capabilities.contains(action.getCapability().getName()))
                .map(action -> {
                    Map<String, Object> actionData = new HashMap<>();
                    actionData.put("name", action.getAction());
                    actionData.put("label", action.getLabel());
                    actionData.put("capability", action.getCapability().getName());
                    actionData.put("icon", action.getIcon());
                    actionData.put("variant", action.getVariant());
                    return actionData;
                })
                .collect(Collectors.toList());

            if (!userActions.isEmpty()) {
                Map<String, Object> pageData = new HashMap<>();
                pageData.put("id", page.getId());
                pageData.put("name", page.getLabel());
                pageData.put("path", page.getRoute());
                pageData.put("parentId", page.getParentId());
                pageData.put("icon", page.getIcon());
                pageData.put("displayOrder", page.getDisplayOrder());
                pageData.put("isMenuItem", page.getIsMenuItem());
                pageData.put("actions", userActions);

                accessiblePages.add(pageData);
                accessiblePageIds.add(page.getId());
            }
        }

        // Second pass: add parent pages that don't have actions but have accessible children
        Set<Long> parentIdsToAdd = new HashSet<>();
        for (Map<String, Object> pageData : accessiblePages) {
            Long parentId = (Long) pageData.get("parentId");
            if (parentId != null && !accessiblePageIds.contains(parentId)) {
                parentIdsToAdd.add(parentId);
            }
        }

        // Add parent pages
        for (Long parentId : parentIdsToAdd) {
            UIPage parentPage = allPages.stream()
                    .filter(p -> p.getId().equals(parentId))
                    .findFirst()
                    .orElse(null);
            
            if (parentPage != null) {
                Map<String, Object> parentData = new HashMap<>();
                parentData.put("id", parentPage.getId());
                parentData.put("name", parentPage.getLabel());
                parentData.put("path", parentPage.getRoute());
                parentData.put("parentId", parentPage.getParentId());
                parentData.put("icon", parentPage.getIcon());
                parentData.put("displayOrder", parentPage.getDisplayOrder());
                parentData.put("isMenuItem", parentPage.getIsMenuItem());
                parentData.put("actions", new ArrayList<>()); // No direct actions

                accessiblePages.add(parentData);
                accessiblePageIds.add(parentPage.getId());
            }
        }

        // Sort pages: parents first (by displayOrder), then children (by displayOrder)
        accessiblePages.sort((a, b) -> {
            Long parentIdA = (Long) a.get("parentId");
            Long parentIdB = (Long) b.get("parentId");
            Integer displayOrderA = (Integer) a.getOrDefault("displayOrder", 0);
            Integer displayOrderB = (Integer) b.getOrDefault("displayOrder", 0);
            
            // Both are root pages - sort by displayOrder
            if (parentIdA == null && parentIdB == null) {
                return Integer.compare(displayOrderA, displayOrderB);
            }
            
            // A is root, B is child - A comes first
            if (parentIdA == null && parentIdB != null) {
                return -1;
            }
            
            // A is child, B is root - B comes first
            if (parentIdA != null && parentIdB == null) {
                return 1;
            }
            
            // Both are children (neither is null at this point)
            // Check if same parent
            if (parentIdA != null && parentIdA.equals(parentIdB)) {
                return Integer.compare(displayOrderA, displayOrderB);
            }
            
            // Different parents - group by parent's display order
            Integer parentOrderA = accessiblePages.stream()
                    .filter(p -> p.get("id").equals(parentIdA))
                    .map(p -> (Integer) p.getOrDefault("displayOrder", 0))
                    .findFirst()
                    .orElse(0);
            Integer parentOrderB = accessiblePages.stream()
                    .filter(p -> p.get("id").equals(parentIdB))
                    .map(p -> (Integer) p.getOrDefault("displayOrder", 0))
                    .findFirst()
                    .orElse(0);
            
            return Integer.compare(parentOrderA, parentOrderB);
        });

        logger.debug("User has access to {} pages (including parent pages)", accessiblePages.size());
        return accessiblePages;
    }

    /**
     * Build capability map for quick frontend checks
     * { "USER_CREATE": true, "USER_DELETE": true, ... }
     */
    private Map<String, Boolean> buildCapabilityMap(Set<String> capabilities) {
        Map<String, Boolean> capabilityMap = new HashMap<>();
        for (String capability : capabilities) {
            capabilityMap.put(capability, true);
        }
        return capabilityMap;
    }
}
