package com.example.userauth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit log for authorization decisions.
 * Tracks access attempts, decisions, and reasons for security auditing.
 */
@Entity
@Table(name = "authorization_audit")
public class AuthorizationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "endpoint_key", length = 100)
    private String endpointKey; // Which endpoint was accessed

    @Column(name = "http_method", length = 10)
    private String httpMethod; // GET, POST, etc.

    @Column(name = "request_path", length = 255)
    private String requestPath; // Actual path requested

    @Column(name = "required_capability", length = 100)
    private String requiredCapability; // Capability that was checked

    @Column(name = "decision", nullable = false, length = 20)
    private String decision; // GRANTED, DENIED

    @Column(name = "reason", length = 500)
    private String reason; // Why access was granted/denied

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // Client IP address

    @Column(name = "user_agent", length = 255)
    private String userAgent; // Client user agent

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuthorizationAudit() {
    }

    public AuthorizationAudit(Long userId, String username, String endpointKey, 
                             String httpMethod, String requestPath, String requiredCapability, 
                             String decision, String reason) {
        this.userId = userId;
        this.username = username;
        this.endpointKey = endpointKey;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.requiredCapability = requiredCapability;
        this.decision = decision;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getEndpointKey() {
        return endpointKey;
    }

    public void setEndpointKey(String endpointKey) {
        this.endpointKey = endpointKey;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequiredCapability() {
        return requiredCapability;
    }

    public void setRequiredCapability(String requiredCapability) {
        this.requiredCapability = requiredCapability;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AuthorizationAudit{" +
                "id=" + id +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", endpointKey='" + endpointKey + '\'' +
                ", decision='" + decision + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
