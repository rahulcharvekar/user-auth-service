package com.example.userauth.controller;

import com.example.userauth.dao.RoleQueryDao.RoleWithPermissionCount;
import com.example.userauth.entity.Role;
import com.example.userauth.entity.User;
import com.example.userauth.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import com.shared.common.util.ETagUtil;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import com.shared.common.annotation.Auditable;

/**
 * Admin controller for managing roles
 */
@RestController
@RequestMapping("/api/admin/roles")
@Tag(name = "Role Management", description = "APIs for managing roles")
@SecurityRequirement(name = "Bearer Authentication")
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleService roleService;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Auditable(action = "GET_ALL_ROLES", resourceType = "ROLE")
    @GetMapping
    @Operation(summary = "Get all roles")
    public ResponseEntity<List<Role>> getAllRoles(HttpServletRequest request) {
        List<Role> roles = roleService.getAllRoles();
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
    
    @GetMapping("/with-permissions")
    @Operation(summary = "Get all roles with permissions")
    public ResponseEntity<List<RoleWithPermissionCount>> getAllRolesWithPermissions(HttpServletRequest request) {
        List<RoleWithPermissionCount> roles = roleService.getAllRolesWithPermissionCounts();
        try {
            String responseJson = objectMapper.writeValueAsString(roles);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(roles);
        } catch (Exception e) {
            logger.error("Error processing roles with permissions response", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id, HttpServletRequest request) {
        return roleService.getRoleById(id)
                .map(role -> {
                    try {
                        String responseJson = objectMapper.writeValueAsString(role);
                        String eTag = ETagUtil.generateETag(responseJson);
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (eTag.equals(ifNoneMatch)) {
                            return (ResponseEntity<Role>) (ResponseEntity<?>) ResponseEntity.status(304).eTag(eTag).build();
                        }
                        return (ResponseEntity<Role>) (ResponseEntity<?>) ResponseEntity.ok().eTag(eTag).body(role);
                    } catch (Exception e) {
                        logger.error("Error processing role response", e);
                        return (ResponseEntity<Role>) (ResponseEntity<?>) ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/by-name/{name}")
    @Operation(summary = "Get role by name with permissions")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Role> getRoleByName(@PathVariable String name, HttpServletRequest request) {
        return roleService.getRoleByNameWithPermissions(name)
                .map(role -> {
                    try {
                        String responseJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(role);
                        String eTag = ETagUtil.generateETag(responseJson);
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (eTag.equals(ifNoneMatch)) {
                            return (ResponseEntity<Role>) (ResponseEntity<?>) ResponseEntity.status(304).eTag(eTag).build();
                        }
                        return (ResponseEntity<Role>) (ResponseEntity<?>) ResponseEntity.ok().eTag(eTag).body(role);
                    } catch (Exception e) {
                        return (ResponseEntity<Role>) (ResponseEntity<?>) ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "Create new role")
    public ResponseEntity<Role> createRole(@RequestBody CreateRoleRequest request) {
        try {
            Role role = roleService.createRole(request.getName(), request.getDescription());
            return ResponseEntity.ok(role);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update role")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, 
                                           @RequestBody UpdateRoleRequest request) {
        try {
            Role role = roleService.updateRole(id, request.getName(), request.getDescription());
            return ResponseEntity.ok(role);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * DEPRECATED: Old permission system endpoints - replaced by Capability+Policy system
     * Use PolicyEngineService and AuthorizationService instead
     */
    
    // @PostMapping("/{roleId}/permissions/{permissionId}")
    // @Operation(summary = "Add permission to role")
    // public ResponseEntity<Role> addPermissionToRole(@PathVariable Long roleId, 
    //                                                  @PathVariable Long permissionId) {
    //     // OLD SYSTEM - use Capability+Policy assignment instead
    //     return ResponseEntity.status(HttpStatus.GONE)
    //             .body(null); // 410 Gone
    // }
    
    // @DeleteMapping("/{roleId}/permissions/{permissionId}")
    // @Operation(summary = "Remove permission from role")
    // public ResponseEntity<Role> removePermissionFromRole(@PathVariable Long roleId, 
    //                                                       @PathVariable Long permissionId) {
    //     // OLD SYSTEM - use Capability+Policy assignment instead
    //     return ResponseEntity.status(HttpStatus.GONE)
    //             .body(null); // 410 Gone
    // }
    
    @PostMapping("/assign")
    @Operation(summary = "Assign role to user")
    public ResponseEntity<User> assignRoleToUser(@RequestBody AssignRoleRequest request) {
        try {
            User user = roleService.assignRoleToUser(request.getUserId(), request.getRoleId());
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/revoke")
    @Operation(summary = "Revoke role from user")
    public ResponseEntity<User> revokeRoleFromUser(@RequestBody RevokeRoleRequest request) {
        try {
            User user = roleService.revokeRoleFromUser(request.getUserId(), request.getRoleId());
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Request DTOs
    public static class CreateRoleRequest {
        private String name;
        private String description;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class UpdateRoleRequest {
        private String name;
        private String description;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class AssignRoleRequest {
        private Long userId;
        private Long roleId;
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
    }
    
    public static class RevokeRoleRequest {
        private Long userId;
        private Long roleId;
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
    }
}
