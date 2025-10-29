package com.example.userauth.service;

import com.example.userauth.dto.AuthResponse;
import com.example.userauth.dto.LoginRequest;
import com.example.userauth.dto.RegisterRequest;
import com.example.userauth.entity.User;
import com.example.userauth.entity.UserRole;
import com.example.userauth.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserQueryDao userQueryDao;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtils jwtUtils;
    
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
        // This needs a role lookup first, but we'll implement a simple version
        // In a real scenario, you'd get the role ID first then find users
        return userQueryDao.findAll(); // Placeholder - needs proper implementation
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
}
