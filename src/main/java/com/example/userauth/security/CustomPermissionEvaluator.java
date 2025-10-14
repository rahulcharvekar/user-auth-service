package com.example.userauth.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
    
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String permissionName = "PERM_" + permission.toString();
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(permissionName));
    }
    
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }
    
    /**
     * Check if user has any of the specified permissions
     */
    public boolean hasAnyPermission(Authentication authentication, String... permissions) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        for (String permission : permissions) {
            if (hasPermission(authentication, null, permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if user has all of the specified permissions
     */
    public boolean hasAllPermissions(Authentication authentication, String... permissions) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        for (String permission : permissions) {
            if (!hasPermission(authentication, null, permission)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if user has role
     */
    public boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String roleName = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(roleName));
    }
    
    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        for (String role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }
        return false;
    }
}
