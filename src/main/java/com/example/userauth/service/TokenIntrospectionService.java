package com.example.userauth.service;

import com.example.userauth.dto.internal.TokenIntrospectionResponse;
import com.example.userauth.entity.User;
import com.example.userauth.repository.UserRepository;
import com.example.userauth.security.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class TokenIntrospectionService {

    private static final Logger log = LoggerFactory.getLogger(TokenIntrospectionService.class);

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public TokenIntrospectionService(JwtUtils jwtUtils,
                                     UserRepository userRepository,
                                     TokenBlacklistService tokenBlacklistService) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public TokenIntrospectionResponse introspect(String token) throws JwtException {
        Claims claims = jwtUtils.parseClaims(token);
        TokenIntrospectionResponse response = new TokenIntrospectionResponse();

        response.setSubject(claims.getSubject());
        response.setTokenId(claims.getId());
        if (claims.getExpiration() != null) {
            response.setExpiresAt(claims.getExpiration().toInstant());
        }

        String tokenId = claims.getId();

        if (tokenBlacklistService.isTokenRevoked(tokenId)) {
            log.debug("Introspection inactive because token {} is revoked", tokenId);
            response.setActive(false);
            return response;
        }

        Long tokenUserId = claims.get(JwtUtils.CLAIM_USER_ID, Long.class);
        Integer tokenPermissionVersion = claims.get(JwtUtils.CLAIM_PERMISSION_VERSION, Integer.class);

        Optional<User> userOpt = userRepository.findByUsername(claims.getSubject());

        if (userOpt.isEmpty()) {
            log.debug("Introspection: user {} not found", claims.getSubject());
            response.setActive(false);
            return response;
        }

        User user = userOpt.get();
        response.setUserId(user.getId());
        response.setPermissionVersion(user.getPermissionVersion());

        boolean accountActive = user.isEnabled()
            && user.isAccountNonExpired()
            && user.isAccountNonLocked()
            && user.isCredentialsNonExpired();

        boolean permissionMatches = tokenPermissionVersion != null
            && tokenPermissionVersion.equals(user.getPermissionVersion());

        boolean userMatches = tokenUserId == null || tokenUserId.equals(user.getId());

        Instant now = Instant.now();
        boolean notExpired = claims.getExpiration() == null
            || claims.getExpiration().toInstant().isAfter(now);

        boolean active = accountActive && permissionMatches && userMatches && notExpired;
        response.setActive(active);

        if (!active) {
            log.debug("Introspection inactive for user {} (accountActive={}, permissionMatches={}, userMatches={}, notExpired={})",
                user.getUsername(), accountActive, permissionMatches, userMatches, notExpired);
        }

        return response;
    }
}
