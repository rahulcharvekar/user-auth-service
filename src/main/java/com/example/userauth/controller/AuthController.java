package com.example.userauth.controller;

import com.example.userauth.dto.*;
import com.example.userauth.entity.User;
import com.example.userauth.entity.UserRole;
import com.example.userauth.service.AuthService;
import com.example.userauth.service.UIConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import com.shared.common.util.ETagUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;


import com.shared.common.annotation.Auditable;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Controller for authentication and user management
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UIConfigService uiConfigService;

    @Autowired
    private ObjectMapper objectMapper;
    
    @PostMapping("/login")
    @Auditable(action = "LOGIN_ATTEMPT", resourceType = "USER")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login attempt for user: {}", loginRequest.getUsername());
            AuthResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid username or password"));
        }
    }

    @PostMapping("/logout")
    @Auditable(action = "LOGOUT", resourceType = "USER")
    @Operation(summary = "User logout", description = "Revoke the current JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "400", description = "Authorization header missing"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Authorization header with Bearer token is required"));
        }

        String token = authHeader.substring(7);

        try {
            authService.logout(token);
            return ResponseEntity.ok(Map.of("message", "Logout successful"));
        } catch (IllegalArgumentException e) {
            logger.warn("Logout failed due to missing token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Logout failed for token", e);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        }
    }
    
    @PostMapping("/register")
    @Auditable(action = "REGISTER_ATTEMPT", resourceType = "USER")
    @Operation(summary = "User registration", description = "Register a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Registration failed - username or email already exists")
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            logger.info("Registration attempt for user: {}", registerRequest.getUsername());
            AuthResponse response = authService.register(registerRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Registration failed for user: {}", registerRequest.getUsername(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    
    @Auditable(action = "GET_UI_CONFIG", resourceType = "USER")
    @GetMapping("/ui-config")
    @Operation(summary = "Get UI configuration", description = "Get complete UI configuration including navigation and permissions for current user")
    @ApiResponse(responseCode = "200", description = "UI configuration retrieved successfully")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getUserUIConfig(HttpServletRequest request) {
        try {
            PermissionResponse uiConfig = uiConfigService.getUserUIConfig();
            if (uiConfig != null) {
                String responseJson = objectMapper.writeValueAsString(uiConfig);
                String eTag = ETagUtil.generateETag(responseJson);
                String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                if (eTag.equals(ifNoneMatch)) {
                    return ResponseEntity.status(304).eTag(eTag).build();
                }
                return ResponseEntity.ok().eTag(eTag).body(uiConfig);
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "User not authenticated"));
            }
        } catch (Exception e) {
            logger.error("Failed to get UI configuration", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to retrieve UI configuration"));
        }
    }
    
        @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Get list of all users (Requires authentication)")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    @Auditable(action = "GET_ALL_USERS", resourceType = "USER")
    public ResponseEntity<List<User>> getAllUsers(HttpServletRequest request) {
        List<User> users = authService.getAllUsers();
        try {
            String responseJson = objectMapper.writeValueAsString(users);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(users);
        } catch (Exception e) {
            logger.error("Error processing users response", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Auditable(action = "GET_USERS_BY_ROLE", resourceType = "USER")
    @GetMapping("/users/role/{role}")
    @Operation(summary = "Get users by role", description = "Get users filtered by role (Requires authentication)")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<User>> getUsersByRole(
        @Parameter(description = "User role") @PathVariable UserRole role,
        HttpServletRequest request) {
        List<User> users = authService.getUsersByRole(role);
        try {
            String responseJson = objectMapper.writeValueAsString(users);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(users);
        } catch (Exception e) {
            logger.error("Error processing users by role response", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Auditable(action = "UPDATE_USER_STATUS", resourceType = "USER")
    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Update user status", description = "Enable or disable user account (Requires authentication)")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> updateUserStatus(
        @Parameter(description = "User ID") @PathVariable Long userId,
        @Parameter(description = "Enable/disable user") @RequestParam boolean enabled) {
        try {
            authService.updateUserStatus(userId, enabled);
            return ResponseEntity.ok(Map.of(
                "message", "User status updated successfully",
                "userId", userId,
                "enabled", enabled
            ));
        } catch (Exception e) {
            logger.error("Failed to update user status", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Auditable(action = "UPDATE_USER_ROLES", resourceType = "USER")
    @PutMapping("/users/{userId}/roles")
    @Operation(summary = "Update user roles", description = "Update user's roles and invalidate existing tokens (Requires authentication)")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> updateUserRoles(
        @Parameter(description = "User ID") @PathVariable Long userId,
        @Parameter(description = "Role IDs") @RequestBody java.util.Set<Long> roleIds) {
        try {
            authService.updateUserPermissions(userId);
            return ResponseEntity.ok(Map.of(
                "message", "User roles updated successfully. All existing tokens have been invalidated.",
                "userId", userId
            ));
        } catch (Exception e) {
            logger.error("Failed to update user roles", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Auditable(action = "INVALIDATE_USER_TOKENS", resourceType = "USER")
    @PostMapping("/users/{userId}/invalidate-tokens")
    @Operation(summary = "Invalidate user tokens", description = "Manually invalidate all JWT tokens for a user (Requires authentication)")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> invalidateUserTokens(
        @Parameter(description = "User ID") @PathVariable Long userId) {
        try {
            authService.updateUserPermissions(userId);
            return ResponseEntity.ok(Map.of(
                "message", "All tokens for user have been invalidated. User must re-login.",
                "userId", userId
            ));
        } catch (Exception e) {
            logger.error("Failed to invalidate user tokens", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Auditable(action = "GET_AVAILABLE_ROLES", resourceType = "ROLE")
    @GetMapping("/roles")
    @Operation(summary = "Get available roles", description = "Get list of available user roles")
    public ResponseEntity<UserRole[]> getAvailableRoles(HttpServletRequest request) {
        UserRole[] roles = UserRole.values();
        try {
            String responseJson = objectMapper.writeValueAsString(roles);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(roles);
        } catch (Exception e) {
            logger.error("Error processing roles response", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
