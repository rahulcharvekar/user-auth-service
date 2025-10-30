package com.example.userauth.service;

import com.example.userauth.entity.RevokedToken;
import com.example.userauth.repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Persists identifiers for JWT tokens that have been explicitly revoked.
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public void revokeToken(String tokenId, Long userId, Instant expiresAt) {
        if (!StringUtils.hasText(tokenId) || expiresAt == null) {
            logger.warn("Skipping token revocation due to missing token id or expiration");
            return;
        }

        // Clean up any entries that have already expired to keep the table small.
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());

        revokedTokenRepository.findByTokenId(tokenId).ifPresentOrElse(existing -> {
            existing.setExpiresAt(expiresAt);
            logger.debug("Token {} already revoked. Updated expiration to {}", tokenId, expiresAt);
        }, () -> {
            revokedTokenRepository.save(new RevokedToken(tokenId, userId, expiresAt));
            logger.debug("Token {} revoked until {}", tokenId, expiresAt);
        });
    }

    @Transactional(readOnly = true)
    public boolean isTokenRevoked(String tokenId) {
        if (!StringUtils.hasText(tokenId)) {
            return false;
        }
        return revokedTokenRepository.existsByTokenId(tokenId);
    }
}
