package com.example.userauth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Junction table linking Users to Roles (Many-to-Many relationship).
 * Users can have multiple roles in the new authorization system.
 */
@Entity
@Table(name = "user_roles")
public class UserRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    public UserRoleAssignment() {
    }

    public UserRoleAssignment(User user, Role role) {
        this.user = user;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    @Override
    public String toString() {
        return "UserRoleAssignment{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", roleId=" + (role != null ? role.getId() : null) +
                ", assignedAt=" + assignedAt +
                '}';
    }
}
