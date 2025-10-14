package com.example.userauth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a granular capability/permission in the system.
 * Capabilities define WHAT actions can be performed.
 * Examples: USER_READ, PAYMENT_APPROVE, WORKER_UPLOAD
 */
@Entity
@Table(name = "capabilities")
public class Capability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 50)
    private String module;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 100)
    private String resource;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Capability() {
    }

    public Capability(String name, String description, String module, String action, String resource) {
        this.name = name;
        this.description = description;
        this.module = module;
        this.action = action;
        this.resource = resource;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
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

    /**
     * Extract action from capability name (e.g., USER_READ -> READ)
     */
    public String extractAction() {
        if (name != null && name.contains("_")) {
            String[] parts = name.split("_");
            return parts[parts.length - 1];
        }
        return action;
    }

    /**
     * Check if this capability matches a given module and action
     */
    public boolean matches(String module, String action) {
        return this.module.equalsIgnoreCase(module) && 
               this.action.equalsIgnoreCase(action) && 
               this.isActive;
    }

    @Override
    public String toString() {
        return "Capability{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", module='" + module + '\'' +
                ", action='" + action + '\'' +
                ", resource='" + resource + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
