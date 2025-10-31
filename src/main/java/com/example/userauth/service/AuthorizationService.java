package com.example.userauth.service;

import com.example.userauth.entity.Endpoint;
import com.example.userauth.entity.PageAction;
import com.example.userauth.entity.UIPage;
import com.example.userauth.entity.User;
import com.example.userauth.entity.UserRoleAssignment;
import com.example.userauth.repository.CapabilityRepository;
import com.example.userauth.repository.EndpointPolicyRepository;
import com.example.userauth.repository.EndpointRepository;
import com.example.userauth.repository.PageActionRepository;
import com.example.userauth.repository.PolicyRepository;
import com.example.userauth.repository.UIPageRepository;
import com.example.userauth.repository.UserRepository;
import com.example.userauth.repository.UserRoleAssignmentRepository;
import com.example.userauth.service.dto.AuthorizationMatrix;
import com.example.userauth.service.dto.EndpointAuthorizationMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
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
    private final PolicyRepository policyRepository;
    private final CapabilityRepository capabilityRepository;
    private final EndpointRepository endpointRepository;
    private final EndpointPolicyRepository endpointPolicyRepository;
    private final UIPageRepository uiPageRepository;
    private final PageActionRepository pageActionRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final long ENDPOINT_CACHE_TTL_MS = 30_000L;
    private static final long POLICY_CAPABILITIES_CACHE_TTL_MS = 30_000L;

    private final Map<String, List<EndpointDescriptor>> endpointCache = new ConcurrentHashMap<>();
    private final AtomicLong endpointCacheLoadedAt = new AtomicLong(0);
    private final Map<Long, CapabilitiesCacheEntry> policyCapabilitiesCache = new ConcurrentHashMap<>();

    public AuthorizationService(
            UserRepository userRepository,
            UserRoleAssignmentRepository userRoleRepository,
            PolicyRepository policyRepository,
            CapabilityRepository capabilityRepository,
            EndpointRepository endpointRepository,
            EndpointPolicyRepository endpointPolicyRepository,
            UIPageRepository uiPageRepository,
            PageActionRepository pageActionRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.policyRepository = policyRepository;
        this.capabilityRepository = capabilityRepository;
        this.endpointRepository = endpointRepository;
        this.endpointPolicyRepository = endpointPolicyRepository;
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

        AuthorizationMatrix matrix = buildAuthorizationMatrix(user);

        logger.debug("User {} has roles: {}", userId, matrix.getRoles());

        List<Map<String, Object>> pages = getAccessiblePagesFilteredByCapabilities(matrix.getRoles(), matrix.getCapabilities());

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("username", user.getUsername());
        response.put("roles", matrix.getRoles());
        response.put("can", buildCapabilityMap(matrix.getCapabilities()));
        response.put("pages", pages);
        response.put("version", System.currentTimeMillis());

        logger.debug("Authorization response built successfully for user: {}", userId);
        return response;
    }

    /**
     * Build an authorization matrix for backend enforcement.
     * This reuses the same logic used for the UI payload.
     */
    @Transactional(readOnly = true)
    public AuthorizationMatrix buildAuthorizationMatrix(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return buildAuthorizationMatrix(user);
    }

    private AuthorizationMatrix buildAuthorizationMatrix(User user) {
        List<UserRoleAssignment> userRoles = userRoleRepository.findByUserId(user.getId());
        Set<String> roleNames = userRoles.stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());
        Set<String> capabilities = getCapabilitiesForRoles(roleNames);
        return new AuthorizationMatrix(user.getId(), user.getPermissionVersion(), roleNames, capabilities);
    }

    /**
     * Resolve the capability names that guard a specific endpoint definition.
     * Returns an empty set if the endpoint is not cataloged or has no policies.
     */
    @Transactional(readOnly = true)
    public EndpointAuthorizationMetadata getEndpointAuthorizationMetadata(String httpMethod, String requestPath) {
        String normalizedMethod = httpMethod != null ? httpMethod.toUpperCase(Locale.ROOT) : "GET";
        String normalizedPath = normalizePath(requestPath);

        Optional<EndpointDescriptor> endpointOpt = findMatchingEndpoint(normalizedMethod, normalizedPath);
        if (endpointOpt.isEmpty()) {
            logger.debug("No endpoint catalog match for method={} path={}", normalizedMethod, normalizedPath);
            return new EndpointAuthorizationMetadata(false, null, false, Set.of(), Set.of());
        }

        EndpointDescriptor endpoint = endpointOpt.get();
        if (!endpoint.active()) {
            logger.debug("Endpoint {} is inactive, denying by default", endpoint.id());
            return new EndpointAuthorizationMetadata(true, endpoint.id(), false, Set.of(), Set.of());
        }

        Set<Long> policyIds = endpointPolicyRepository.findByEndpointId(endpoint.id()).stream()
                .map(ep -> ep.getPolicy() != null ? ep.getPolicy().getId() : null)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (policyIds.isEmpty()) {
            logger.debug("Endpoint {} has no policies linked", endpoint.id());
            return new EndpointAuthorizationMetadata(true, endpoint.id(), false, Set.of(), Set.of());
        }

        Set<String> capabilities = resolveCapabilities(policyIds);

        return new EndpointAuthorizationMetadata(true, endpoint.id(), true, policyIds, capabilities);
    }

    private Optional<EndpointDescriptor> findMatchingEndpoint(String method, String normalizedPath) {
        List<EndpointDescriptor> candidates = getEndpointsForMethod(method);
        for (EndpointDescriptor endpoint : candidates) {
            if (!endpoint.active()) {
                continue;
            }
            String endpointPath = normalizePath(endpoint.path());
            if (pathMatcher.match(endpointPath, normalizedPath)) {
                return Optional.of(endpoint);
            }
            for (String candidate : buildCompositePaths(endpoint, endpointPath)) {
                if (pathMatcher.match(candidate, normalizedPath)) {
                    return Optional.of(endpoint);
                }
            }
        }
        return Optional.empty();
    }

    private List<EndpointDescriptor> getEndpointsForMethod(String method) {
        long now = System.currentTimeMillis();
        if (now - endpointCacheLoadedAt.get() >= ENDPOINT_CACHE_TTL_MS) {
            endpointCache.clear();
            endpointCacheLoadedAt.set(now);
        }
        return endpointCache.computeIfAbsent(method, this::loadEndpointsForMethod);
    }

    private List<EndpointDescriptor> loadEndpointsForMethod(String method) {
        return endpointRepository.findByMethod(method).stream()
                .map(endpoint -> new EndpointDescriptor(
                        endpoint.getId(),
                        endpoint.getPath(),
                        endpoint.getService(),
                        endpoint.getVersion(),
                        Boolean.TRUE.equals(endpoint.getIsActive())))
                .collect(Collectors.toList());
    }

    private Set<String> resolveCapabilities(Set<Long> policyIds) {
        if (policyIds.isEmpty()) {
            return Collections.emptySet();
        }

        long now = System.currentTimeMillis();
        Set<String> aggregated = new HashSet<>();
        Set<Long> missing = new HashSet<>();

        for (Long policyId : policyIds) {
            CapabilitiesCacheEntry cached = policyCapabilitiesCache.get(policyId);
            if (cached != null && !cached.isStale(now)) {
                aggregated.addAll(cached.capabilities());
            } else {
                if (cached != null) {
                    policyCapabilitiesCache.remove(policyId);
                }
                missing.add(policyId);
            }
        }

        if (!missing.isEmpty()) {
            Map<Long, Set<String>> fetched = fetchCapabilities(missing);
            for (Map.Entry<Long, Set<String>> entry : fetched.entrySet()) {
                Set<String> caps = entry.getValue();
                policyCapabilitiesCache.put(entry.getKey(), new CapabilitiesCacheEntry(Set.copyOf(caps), now));
                aggregated.addAll(caps);
            }
        }

        return aggregated;
    }

    private Map<Long, Set<String>> fetchCapabilities(Set<Long> policyIds) {
        if (policyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Set<String>> result = new HashMap<>();
        List<PolicyRepository.PolicyCapabilitySummary> rows =
                policyRepository.findCapabilityNamesByPolicyIds(policyIds);
        for (PolicyRepository.PolicyCapabilitySummary row : rows) {
            result.computeIfAbsent(row.getPolicyId(), ignored -> new HashSet<>())
                  .add(row.getCapabilityName());
        }

        for (Long policyId : policyIds) {
            result.computeIfAbsent(policyId, ignored -> Collections.emptySet());
        }
        return result;
    }

    private record EndpointDescriptor(Long id, String path, String service, String version, boolean active) {
    }

    private record CapabilitiesCacheEntry(Set<String> capabilities, long loadedAt) {
        boolean isStale(long now) {
            return now - loadedAt > POLICY_CAPABILITIES_CACHE_TTL_MS;
        }
    }

    private List<String> buildCompositePaths(EndpointDescriptor endpoint, String normalizedEndpointPath) {
        if (!StringUtils.hasText(endpoint.service())) {
            return List.of();
        }

        String serviceSegment = trimSlashes(endpoint.service());
        String versionSegment = trimSlashes(endpoint.version());

        String suffix = normalizedEndpointPath.startsWith("/")
                ? normalizedEndpointPath
                : "/" + normalizedEndpointPath;

        List<String> candidates = new ArrayList<>();

        // /api/{service}/{version}{path}
        StringBuilder builder = new StringBuilder("/api/").append(serviceSegment);
        if (StringUtils.hasText(versionSegment)) {
            builder.append("/").append(versionSegment);
        }
        candidates.add(mergePath(builder.toString(), suffix));

        // /api/{service}{path}
        candidates.add(mergePath("/api/" + serviceSegment, suffix));

        // Ensure unique values and drop any equal to the original path
        return candidates.stream()
                .filter(candidate -> !candidate.equals(normalizedEndpointPath))
                .distinct()
                .toList();
    }

    private String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String mergePath(String base, String suffix) {
        if (base.endsWith("/") && suffix.startsWith("/")) {
            return base.substring(0, base.length() - 1) + suffix;
        }
        if (!base.endsWith("/") && !suffix.startsWith("/")) {
            return base + "/" + suffix;
        }
        return base + suffix;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String normalized = path;
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
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
