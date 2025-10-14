package com.example.userauth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents an action available on a UI page.
 * Actions are buttons/operations that users can perform on a page.
 * Each action requires a specific capability.
 */
@Entity
@Table(name = "page_actions")
public class PageAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String label; // Display name like "Create User"

    @Column(name = "action", nullable = false, length = 64)
    private String action; // Action type: CREATE, EDIT, DELETE, APPROVE, etc.

    @Column(nullable = false, length = 64)
    private String icon; // Icon name

    @Column(nullable = false, length = 32)
    private String variant = "default"; // UI variant: primary, secondary, danger

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capability_id", nullable = false)
    private Capability capability; // Required capability

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private UIPage page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id")
    private Endpoint endpoint; // The backend endpoint to call for this action

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PageAction() {
    }

    public PageAction(String label, String action, Capability capability, UIPage page) {
        this.label = label;
        this.action = action;
        this.capability = capability;
        this.page = page;
        this.isActive = true;
        this.displayOrder = 0;
        this.variant = "default";
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Capability getCapability() {
        return capability;
    }

    public void setCapability(Capability capability) {
        this.capability = capability;
    }

    public UIPage getPage() {
        return page;
    }

    public void setPage(UIPage page) {
        this.page = page;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
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

    @Override
    public String toString() {
        return "PageAction{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", action='" + action + '\'' +
                ", capabilityId=" + (capability != null ? capability.getId() : null) +
                ", pageId=" + (page != null ? page.getId() : null) +
                ", endpointId=" + (endpoint != null ? endpoint.getId() : null) +
                '}';
    }
}
