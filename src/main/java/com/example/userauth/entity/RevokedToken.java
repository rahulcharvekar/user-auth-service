package com.example.userauth.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Stores revoked JWT token identifiers until they expire so requests can be rejected.
 */
@Entity
@Table(name = "revoked_tokens", indexes = {
    @Index(name = "idx_revoked_token_token_id", columnList = "token_id", unique = true),
    @Index(name = "idx_revoked_token_expires_at", columnList = "expires_at")
})
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_id", nullable = false, unique = true, length = 100)
    private String tokenId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RevokedToken() {
        // JPA
    }

    public RevokedToken(String tokenId, Long userId, Instant expiresAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
