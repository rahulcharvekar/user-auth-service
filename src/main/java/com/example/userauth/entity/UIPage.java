package com.example.userauth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a UI page/route in the application.
 * Defines pages available in the frontend navigation.
 */
@Entity
@Table(name = "ui_pages")
public class UIPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_id", nullable = false, unique = true, length = 100)
    private String key; // e.g., "user.list"

    @Column(nullable = false, length = 100)
    private String label; // Display name

    @Column(nullable = false, length = 255)
    private String route; // Frontend route path

    @Column(length = 100)
    private String icon; // Icon identifier

    @Column(nullable = false, length = 50)
    private String module;

    @Column(name = "parent_id")
    private Long parentId; // For hierarchical menu structure

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_menu_item", nullable = false)
    private Boolean isMenuItem = true;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "required_capability", length = 100)
    private String requiredCapability; // The capability required to access this page

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One-to-Many relationship with PageAction
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PageAction> pageActions = new HashSet<>();

    public UIPage() {
    }

    public UIPage(String key, String label, String route, String module) {
        this.key = key;
        this.label = label;
        this.route = route;
        this.module = module;
        this.isMenuItem = true;
        this.isActive = true;
        this.displayOrder = 0;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsMenuItem() {
        return isMenuItem;
    }

    public void setIsMenuItem(Boolean isMenuItem) {
        this.isMenuItem = isMenuItem;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getRequiredCapability() {
        return requiredCapability;
    }

    public void setRequiredCapability(String requiredCapability) {
        this.requiredCapability = requiredCapability;
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

    public Set<PageAction> getPageActions() {
        return pageActions;
    }

    public void setPageActions(Set<PageAction> pageActions) {
        this.pageActions = pageActions;
    }

    @Override
    public String toString() {
        return "UIPage{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", label='" + label + '\'' +
                ", route='" + route + '\'' +
                ", module='" + module + '\'' +
                ", isMenuItem=" + isMenuItem +
                '}';
    }
}
