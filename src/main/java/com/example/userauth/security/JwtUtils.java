package com.example.userauth.security;

import com.example.userauth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    
    @Value("${app.jwt.secret:mySecretKey}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400}")
    private int jwtExpirationMs;
    
    @Value("${app.jwt.issuer:payment-reconciliation-service}")
    private String jwtIssuer;
    
    @Value("${app.jwt.audience:payment-reconciliation-api}")
    private String jwtAudience;
    
    /**
     * Generate JWT token with minimal claims: sub (username), iss, aud, iat, exp, pv (permission version)
     * Permission version is automatically sourced from the User entity
     * @param authentication Spring Security authentication object
     * @return JWT token string
     */
    public String generateJwtToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();
        
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(jwtIssuer)                    // iss: token issuer
                .subject(userPrincipal.getUsername()) // sub: user identifier (username)
                .audience().add(jwtAudience).and()    // aud: intended recipient
                .issuedAt(Date.from(now))             // iat: token creation time
                .expiration(Date.from(now.plus(jwtExpirationMs, ChronoUnit.SECONDS))) // exp: expiration
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }
    
    public String generateTokenFromUsername(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(jwtIssuer)
                .subject(username)
                .audience().add(jwtAudience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtExpirationMs, ChronoUnit.SECONDS)))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }
    
    private SecretKey getSigningKey() {
        try {
            // Try hex decoding first (for hex strings like in config)
            byte[] keyBytes = hexStringToByteArray(jwtSecret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            // Fallback to base64 decoding
            try {
                byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
                return Keys.hmacShaKeyFor(keyBytes);
            } catch (Exception ex) {
                // Use string directly if both fail
                return Keys.hmacShaKeyFor(jwtSecret.getBytes());
            }
        }
    }
    
    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
    
    public String getUserNameFromJwtToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .requireIssuer(jwtIssuer)
                    .requireAudience(jwtAudience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from JWT: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract permission version from JWT token
     * @param token JWT token string
     * @return permission version number or null if not found
     */
    public Integer getPermissionVersionFromToken(String token) {
        try {
            io.jsonwebtoken.Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .requireIssuer(jwtIssuer)
                    .requireAudience(jwtAudience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.get("pv", Integer.class);
        } catch (Exception e) {
            logger.error("Cannot get permission version from JWT token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * @deprecated Roles are no longer stored in JWT. Use UserService to fetch roles from database.
     * This method now returns an empty set for backward compatibility.
     */
    @Deprecated
    public java.util.Set<String> getRolesFromToken(String token) {
        logger.warn("getRolesFromToken() is deprecated. Roles should be fetched from database using username.");
        return new java.util.HashSet<>();
    }
    
    /**
     * @deprecated Permissions are no longer stored in JWT. Use UserService to fetch permissions from database.
     * This method now returns an empty set for backward compatibility.
     */
    @Deprecated
    public java.util.Set<String> getPermissionsFromToken(String token) {
        logger.warn("getPermissionsFromToken() is deprecated. Permissions should be fetched from database using username.");
        return new java.util.HashSet<>();
    }
    
    /**
     * Validate JWT token signature, expiration, issuer, and audience
     * @param authToken JWT token string
     * @return true if valid, false otherwise
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(jwtIssuer)           // Validate issuer
                .requireAudience(jwtAudience)       // Validate audience
                .build()
                .parseSignedClaims(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException e) {
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
    
    /**
     * Validate JWT token and check if permission version matches the current user's permission version
     * @param authToken JWT token string
     * @param currentPermissionVersion Current permission version from database
     * @return true if valid and permission version matches, false otherwise
     */
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
}
