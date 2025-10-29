package com.example.userauth.dto.internal;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenIntrospectionResponse {

    private boolean active;
    private String subject;
    private Long userId;
    private Integer permissionVersion;
    private String tokenId;
    private Instant expiresAt;

    public static TokenIntrospectionResponse inactive() {
        TokenIntrospectionResponse response = new TokenIntrospectionResponse();
        response.setActive(false);
        return response;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPermissionVersion() {
        return permissionVersion;
    }

    public void setPermissionVersion(Integer permissionVersion) {
        this.permissionVersion = permissionVersion;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
