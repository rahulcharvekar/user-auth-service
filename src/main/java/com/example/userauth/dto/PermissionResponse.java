package com.example.userauth.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PermissionResponse {
    private Long userId;
    private String username;
    private String fullName;
    private Set<String> roles;
    private Set<String> permissions;
    private Map<String, Object> uiConfig;
    private List<NavigationItem> navigation;
    
    // Constructors
    public PermissionResponse() {}
    
    public PermissionResponse(Long userId, String username, String fullName, 
                            Set<String> roles, Set<String> permissions,
                            Map<String, Object> uiConfig, List<NavigationItem> navigation) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.roles = roles;
        this.permissions = permissions;
        this.uiConfig = uiConfig;
        this.navigation = navigation;
    }
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
    
    public Map<String, Object> getUiConfig() { return uiConfig; }
    public void setUiConfig(Map<String, Object> uiConfig) { this.uiConfig = uiConfig; }
    
    public List<NavigationItem> getNavigation() { return navigation; }
    public void setNavigation(List<NavigationItem> navigation) { this.navigation = navigation; }
    
    // Inner class for navigation items
    public static class NavigationItem {
        private String id;
        private String label;
        private String path;
        private String icon;
        private String section;
        private List<NavigationItem> children;
        private Set<String> requiredPermissions;
        
        // Constructors
        public NavigationItem() {}
        
        public NavigationItem(String id, String label, String path, String icon, 
                            String section, Set<String> requiredPermissions) {
            this.id = id;
            this.label = label;
            this.path = path;
            this.icon = icon;
            this.section = section;
            this.requiredPermissions = requiredPermissions;
        }
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        
        public String getSection() { return section; }
        public void setSection(String section) { this.section = section; }
        
        public List<NavigationItem> getChildren() { return children; }
        public void setChildren(List<NavigationItem> children) { this.children = children; }
        
        public Set<String> getRequiredPermissions() { return requiredPermissions; }
        public void setRequiredPermissions(Set<String> requiredPermissions) { this.requiredPermissions = requiredPermissions; }
    }
}
