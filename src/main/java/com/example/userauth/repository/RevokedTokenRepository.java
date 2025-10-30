package com.example.userauth.repository;

import com.example.userauth.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    Optional<RevokedToken> findByTokenId(String tokenId);

    boolean existsByTokenId(String tokenId);

    void deleteByExpiresAtBefore(Instant expiresAt);
}
