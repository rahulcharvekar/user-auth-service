package com.example.userauth.dto;

import com.example.userauth.entity.UserRole;

import java.time.Instant;

public class AuthResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private UserRole role;
    private String tokenId;
    private Integer permissionVersion;
    private Instant expiresAt;
    
    // Constructors
    public AuthResponse() {}
    
    public AuthResponse(String token, Long id, String username, String email, String fullName, UserRole role) {
        this(token, id, username, email, fullName, role, null, null, null);
    }

    public AuthResponse(String token,
                        Long id,
                        String username,
                        String email,
                        String fullName,
                        UserRole role,
                        String tokenId,
                        Integer permissionVersion,
                        Instant expiresAt) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.tokenId = tokenId;
        this.permissionVersion = permissionVersion;
        this.expiresAt = expiresAt;
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public Integer getPermissionVersion() {
        return permissionVersion;
    }

    public void setPermissionVersion(Integer permissionVersion) {
        this.permissionVersion = permissionVersion;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
