package com.example.userauth.service;

import com.example.userauth.dto.AuthResponse;
import com.example.userauth.dto.LoginRequest;
import com.example.userauth.dto.RegisterRequest;
import com.example.userauth.entity.Role;
import com.example.userauth.entity.User;
import com.example.userauth.entity.UserRole;
import com.example.userauth.repository.UserRepository;
import com.example.userauth.repository.RoleRepository;
import com.example.userauth.security.JwtUtils;
import com.example.userauth.dao.UserQueryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserQueryDao userQueryDao;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("Attempting login for user: {}", loginRequest.getUsername());
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), 
                loginRequest.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        User user = (User) authentication.getPrincipal();
        
        // Generate JWT token with user's current permission version (auto-sourced from User entity)
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        logger.info("User {} logged in successfully", user.getUsername());
        
        return new AuthResponse(
            jwt,
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            jwtUtils.getTokenId(jwt),
            user.getPermissionVersion(),
            jwtUtils.getExpirationInstant(jwt)
        );
    }

    public void logout(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new IllegalArgumentException("JWT token is required for logout");
        }

        if (!jwtUtils.validateJwtToken(rawToken)) {
            throw new RuntimeException("Invalid or expired JWT token");
        }

        String tokenId = jwtUtils.getTokenId(rawToken);
        Instant expiresAt = jwtUtils.getExpirationInstant(rawToken);
        Long userId = jwtUtils.getUserIdFromToken(rawToken);

        if (tokenId == null) {
            throw new RuntimeException("Unable to extract token identifier");
        }

        if (expiresAt == null) {
            expiresAt = Instant.now();
        }

        tokenBlacklistService.revokeToken(tokenId, userId, expiresAt);
        SecurityContextHolder.clearContext();
        logger.info("Token {} revoked successfully for logout", tokenId);
    }
    
    public AuthResponse register(RegisterRequest registerRequest) {
        logger.info("Attempting registration for user: {}", registerRequest.getUsername());
        
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }
        
        // Create new user account with default permission version 1
        User user = new User(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            passwordEncoder.encode(registerRequest.getPassword()),
            registerRequest.getFullName(),
            registerRequest.getRole() != null ? registerRequest.getRole() : UserRole.WORKER
        );

        userRepository.save(user);
        
        logger.info("User {} registered successfully", user.getUsername());
        
        // Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                registerRequest.getUsername(), 
                registerRequest.getPassword())
        );
        
        // Generate JWT token with user's permission version (auto-sourced from User entity)
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        return new AuthResponse(
            jwt,
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            jwtUtils.getTokenId(jwt),
            user.getPermissionVersion(),
            jwtUtils.getExpirationInstant(jwt)
        );
    }
    
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return Optional.of((User) authentication.getPrincipal());
        }
        return Optional.empty();
    }
    
    // READ OPERATIONS - Using Query DAO
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        logger.debug("Fetching all users using query DAO");
        return userQueryDao.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(UserRole role) {
        logger.debug("Fetching users by role: {} using query DAO", role);
        return userQueryDao.findByRole(role);
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByRoleName(String roleName) {
        logger.debug("Fetching users by role name: {} using query DAO", roleName);
        if (!StringUtils.hasText(roleName)) {
            return List.of();
        }

        Map<Long, User> usersById = new LinkedHashMap<>();

        String normalized = roleName.trim();
        try {
            UserRole legacyRole = UserRole.valueOf(normalized.toUpperCase(Locale.ROOT));
            userQueryDao.findByRole(legacyRole)
                    .forEach(user -> usersById.put(user.getId(), user));
        } catch (IllegalArgumentException ignored) {
            // Role name does not map to legacy enum; continue with capability-based lookup
        }

        userQueryDao.findByRoleName(normalized)
                .forEach(user -> usersById.put(user.getId(), user));

        return List.copyOf(usersById.values());
    }
    
    @Transactional(readOnly = true)
    public List<User> getActiveUsers() {
        logger.debug("Fetching active users using query DAO");
        return userQueryDao.findActiveUsers();
    }
    
    @Transactional(readOnly = true)
    public List<User> searchUsers(String searchTerm) {
        logger.debug("Searching users with term: {} using query DAO", searchTerm);
        return userQueryDao.searchUsers(searchTerm);
    }
    
    public void updateUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        boolean previousStatus = user.isEnabled();
        user.setEnabled(enabled);
        if (previousStatus != enabled) {
            user.incrementPermissionVersion();
        }
        userRepository.save(user);
        logger.info("User {} status updated to: {}", user.getUsername(), enabled ? "enabled" : "disabled");
    }
    
    /**
     * Update user roles or permissions and increment permission version
     * This will invalidate all existing JWT tokens for the user
     */
    public void updateUserPermissions(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.incrementPermissionVersion();
        userRepository.save(user);
        
        logger.info("User {} permissions updated", user.getUsername());
    }

    public RoleUpdateResult updateUserRoles(Long userId, Set<Long> roleIds) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Set<Long> requestedRoleIds = roleIds == null
            ? Set.of()
            : roleIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Role> requestedRoles = requestedRoleIds.isEmpty()
            ? List.of()
            : roleRepository.findAllById(requestedRoleIds);

        if (requestedRoles.size() != requestedRoleIds.size()) {
            Set<Long> foundIds = requestedRoles.stream()
                    .map(Role::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Set<Long> missing = requestedRoleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            throw new IllegalArgumentException("Unknown role id(s): " + missing);
        }

        Set<Long> currentRoleIds = user.getRoles().stream()
                .map(Role::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        boolean changed = false;

        for (Role existingRole : Set.copyOf(user.getRoles())) {
            if (!requestedRoleIds.contains(existingRole.getId())) {
                user.removeRole(existingRole);
                changed = true;
            }
        }

        for (Role role : requestedRoles) {
            if (!currentRoleIds.contains(role.getId())) {
                user.addRole(role);
                changed = true;
            }
        }

        resolvePrimaryRole(requestedRoles).ifPresent(user::setRole);

        if (changed) {
            userRepository.save(user);
            logger.info("User {} roles updated to {}", user.getUsername(),
                    user.getRoles().stream()
                            .map(Role::getName)
                            .filter(Objects::nonNull)
                            .toList());
        } else {
            logger.info("User {} roles unchanged", user.getUsername());
        }

        Set<Long> assignedIds = user.getRoles().stream()
                .map(Role::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> assignedNames = user.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new RoleUpdateResult(assignedIds, assignedNames);
    }

    private Optional<UserRole> resolvePrimaryRole(List<Role> roles) {
        for (Role role : roles) {
            String name = role.getName();
            if (name == null) {
                continue;
            }
            try {
                return Optional.of(UserRole.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Role name does not map to legacy enum
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get current user's permission names from authentication context
     */
    public Set<String> getCurrentUserPermissionNames() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> permissions = new HashSet<>();
        
        if (authentication != null && authentication.isAuthenticated()) {
            for (org.springframework.security.core.GrantedAuthority authority : authentication.getAuthorities()) {
                String auth = authority.getAuthority();
                if (auth.startsWith("PERM_")) {
                    permissions.add(auth.substring(5)); // Remove "PERM_" prefix
                }
            }
        }
        
        return permissions;
    }
    
    /**
     * Get current user's role names from authentication context
     */
    public Set<String> getCurrentUserRoleNames() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = new HashSet<>();
        
        if (authentication != null && authentication.isAuthenticated()) {
            for (org.springframework.security.core.GrantedAuthority authority : authentication.getAuthorities()) {
                String auth = authority.getAuthority();
                if (auth.startsWith("ROLE_")) {
                    roles.add(auth.substring(5)); // Remove "ROLE_" prefix
                }
            }
        }
        
        return roles;
    }

    public record RoleUpdateResult(Set<Long> roleIds, Set<String> roleNames) {}
}
