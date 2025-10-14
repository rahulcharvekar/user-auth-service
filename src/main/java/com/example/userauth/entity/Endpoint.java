package com.example.userauth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a service catalog endpoint (API endpoint).
 * Defines WHERE authorization is enforced and maps to policies.
 */
@Entity
@Table(name = "endpoints")
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String service; // Service name, e.g., 'worker', 'payment'

    @Column(nullable = false, length = 64)
    private String version; // API version, e.g., 'v1', 'v2'

    @Column(nullable = false, length = 8)
    private String method; // GET, POST, PUT, DELETE, PATCH

    @Column(nullable = false, length = 256)
    private String path; // API path template, e.g., /api/worker/upload

    @Column(columnDefinition = "TEXT")
    private String description; // Endpoint description

    @Column(name = "ui_type", length = 32)
    private String uiType; // UI usage type: ACTION, LIST, FORM, UPLOAD, etc.

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Many-to-Many relationship with Policy through EndpointPolicy
    @OneToMany(mappedBy = "endpoint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<EndpointPolicy> endpointPolicies = new HashSet<>();

    public Endpoint() {
    }

    public Endpoint(String service, String version, String method, String path, String description) {
        this.service = service;
        this.version = version;
        this.method = method;
        this.path = path;
        this.description = description;
        this.isActive = true;
    }

    public Endpoint(String service, String version, String method, String path, String description, String uiType) {
        this.service = service;
        this.version = version;
        this.method = method;
        this.path = path;
        this.description = description;
        this.uiType = uiType;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUiType() {
        return uiType;
    }

    public void setUiType(String uiType) {
        this.uiType = uiType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<EndpointPolicy> getEndpointPolicies() {
        return endpointPolicies;
    }

    public void setEndpointPolicies(Set<EndpointPolicy> endpointPolicies) {
        this.endpointPolicies = endpointPolicies;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "id=" + id +
                ", service='" + service + '\'' +
                ", version='" + version + '\'' +
                ", method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
