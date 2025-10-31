package com.example.userauth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shared.entityaudit.annotation.EntityAuditEnabled;
import com.shared.entityaudit.descriptor.AbstractAuditableEntity;
import com.shared.entityaudit.listener.SharedEntityAuditListener;

/**
 * Represents an authorization policy in the system.
 * Policies define WHO can perform actions (role-based rules).
 * Contains JSON expression for complex authorization logic.
 */
@Entity
@EntityAuditEnabled
@EntityListeners(SharedEntityAuditListener.class)
@Table(name = "policies")
public class Policy extends AbstractAuditableEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 20)
    private String type; // RBAC, ABAC, CUSTOM

    @Column(nullable = false, columnDefinition = "JSON")
    private String expression; // JSON expression for policy evaluation

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Many-to-Many relationship with Capability
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyCapability> policyCapabilities = new HashSet<>();

    public Policy() {
    }

    public Policy(String name, String description, String type, String expression) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.expression = expression;
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
        return id != null ? id : (Long) super.getId();
    }

    public void setId(Long id) {
        this.id = id;
        super.setId(id);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
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

    public Set<PolicyCapability> getPolicyCapabilities() {
        return policyCapabilities;
    }

    public void setPolicyCapabilities(Set<PolicyCapability> policyCapabilities) {
        this.policyCapabilities = policyCapabilities;
    }

    @Override
    public String entityType() {
        return "POLICY";
    }

    @Override
    @JsonIgnore
    @Transient
    public Map<String, Object> auditState() {
        return auditStateOf(
                "id", id,
                "name", name,
                "description", description,
                "type", type,
                "expression", expression,
                "isActive", isActive,
                "createdAt", createdAt != null ? createdAt.toString() : null,
                "updatedAt", updatedAt != null ? updatedAt.toString() : null
        );
    }

    @Override
    public String toString() {
        return "Policy{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
