package com.example.userauth.security;

import com.example.userauth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {

    public static final String CLAIM_PERMISSION_VERSION = "pv";
    public static final String CLAIM_USER_ID = "uid";

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret:mySecretKey}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400}")
    private int jwtExpirationSeconds;

    @Value("${app.jwt.issuer:payment-reconciliation-service}")
    private String jwtIssuer;

    @Value("${app.jwt.audience:payment-reconciliation-api}")
    private String jwtAudience;

    /**
     * Generate a signed JWT embedding user id, permission version, and token id (jti).
     */
    public String generateJwtToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtExpirationSeconds, ChronoUnit.SECONDS);

        Integer permissionVersion = userPrincipal.getPermissionVersion() != null
            ? userPrincipal.getPermissionVersion()
            : 1;

        return Jwts.builder()
            .issuer(jwtIssuer)
            .subject(userPrincipal.getUsername())
            .audience().add(jwtAudience).and()
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim(CLAIM_USER_ID, userPrincipal.getId())
            .claim(CLAIM_PERMISSION_VERSION, permissionVersion)
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    /**
     * @deprecated Use {@link #generateJwtToken(Authentication)} to include user id and permission version claims.
     */
    @Deprecated(forRemoval = false, since = "0.0.2")
    public String generateTokenFromUsername(String username) {
        logger.warn("generateTokenFromUsername(String) produces a token without user context and should be avoided.");
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(jwtIssuer)
            .subject(username)
            .audience().add(jwtAudience).and()
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(jwtExpirationSeconds, ChronoUnit.SECONDS)))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .requireIssuer(jwtIssuer)
            .requireAudience(jwtAudience)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String getUserNameFromJwtToken(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from JWT: {}", e.getMessage());
            return null;
        }
    }

    public Integer getPermissionVersionFromToken(String token) {
        try {
            return parseClaims(token).get(CLAIM_PERMISSION_VERSION, Integer.class);
        } catch (Exception e) {
            logger.error("Cannot get permission version from JWT token: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            return parseClaims(token).get(CLAIM_USER_ID, Long.class);
        } catch (Exception e) {
            logger.error("Cannot get user id from JWT token: {}", e.getMessage());
            return null;
        }
    }

    public String getTokenId(String token) {
        try {
            return parseClaims(token).getId();
        } catch (Exception e) {
            logger.error("Cannot get token id from JWT token: {}", e.getMessage());
            return null;
        }
    }

    public Instant getExpirationInstant(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration != null ? expiration.toInstant() : null;
        } catch (Exception e) {
            logger.error("Cannot get expiration from JWT token: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateJwtToken(String authToken) {
        try {
            parseClaims(authToken);
            return true;
        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public boolean validateJwtTokenWithPermissionVersion(String authToken, Integer currentPermissionVersion) {
        if (!validateJwtToken(authToken)) {
            return false;
        }

        Integer tokenPv = getPermissionVersionFromToken(authToken);
        if (tokenPv == null || currentPermissionVersion == null) {
            logger.warn("Permission version missing in token or database");
            return false;
        }

        if (!tokenPv.equals(currentPermissionVersion)) {
            logger.info("Permission version mismatch. Token: {}, Current: {}. User permissions have been updated.",
                tokenPv, currentPermissionVersion);
            return false;
        }

        return true;
    }

    private SecretKey getSigningKey() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("JWT secret is not configured");
        }

        try {
            byte[] keyBytes = hexStringToByteArray(jwtSecret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            try {
                byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
                return Keys.hmacShaKeyFor(keyBytes);
            } catch (Exception ex) {
                return Keys.hmacShaKeyFor(jwtSecret.getBytes());
            }
        }
    }

    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
