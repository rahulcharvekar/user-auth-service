package com.example.userauth.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for user information with roles, permissions, and API endpoints
 * Returned by /api/auth/me endpoint
 * 
 * Structure: User → Roles → Permissions → API Endpoints
 */
public class UserPermissionResponse {
    
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private List<RoleInfo> roles;                              // User's roles
    private Map<String, List<PermissionInfo>> permissions;     // Permissions grouped by module
    
    public UserPermissionResponse() {
        this.roles = new ArrayList<>();
        this.permissions = new HashMap<>();
    }
    
    public UserPermissionResponse(Long userId, String username, String email, String fullName) {
        this();
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public List<RoleInfo> getRoles() {
        return roles;
    }
    
    public void setRoles(List<RoleInfo> roles) {
        this.roles = roles;
    }
    
    public Map<String, List<PermissionInfo>> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(Map<String, List<PermissionInfo>> permissions) {
        this.permissions = permissions;
    }
    
    @Override
    public String toString() {
        return "UserPermissionResponse{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", roles=" + roles +
                ", permissions=" + permissions +
                '}';
    }
    
    /**
     * Nested class for role information
     */
    public static class RoleInfo {
        private Long id;
        private String name;
        private String description;
        
        public RoleInfo() {
        }
        
        public RoleInfo(Long id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return "RoleInfo{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
    
    /**
     * Nested class for permission information with API endpoints
     */
    public static class PermissionInfo {
        private Long id;
        private String name;
        private String description;
        private String module;
        private List<ApiEndpoint> apiEndpoints;
        
        public PermissionInfo() {
            this.apiEndpoints = new ArrayList<>();
        }
        
        public PermissionInfo(Long id, String name, String description, String module) {
            this();
            this.id = id;
            this.name = name;
            this.description = description;
            this.module = module;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getModule() {
            return module;
        }
        
        public void setModule(String module) {
            this.module = module;
        }
        
        public List<ApiEndpoint> getApiEndpoints() {
            return apiEndpoints;
        }
        
        public void setApiEndpoints(List<ApiEndpoint> apiEndpoints) {
            this.apiEndpoints = apiEndpoints;
        }
        
        @Override
        public String toString() {
            return "PermissionInfo{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", module='" + module + '\'' +
                    ", apiEndpoints=" + apiEndpoints.size() +
                    '}';
        }
    }
    
    /**
     * Nested class for API endpoint information
     */
    public static class ApiEndpoint {
        private String endpoint;
        private String method;
        private String description;
        
        public ApiEndpoint() {
        }
        
        public ApiEndpoint(String endpoint, String method, String description) {
            this.endpoint = endpoint;
            this.method = method;
            this.description = description;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public String getMethod() {
            return method;
        }
        
        public void setMethod(String method) {
            this.method = method;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return method + " " + endpoint;
        }
    }
}
