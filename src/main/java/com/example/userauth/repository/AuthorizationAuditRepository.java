package com.example.userauth.repository;

import com.example.userauth.entity.AuthorizationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuthorizationAudit entity.
 * Provides methods to query authorization audit logs.
 */
@Repository
public interface AuthorizationAuditRepository extends JpaRepository<AuthorizationAudit, Long> {

    /**
     * Find all audit logs for a specific user
     */
    List<AuthorizationAudit> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all audit logs for a specific user within a date range
     */
    List<AuthorizationAudit> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find all denied access attempts
     */
    List<AuthorizationAudit> findByDecisionOrderByCreatedAtDesc(String decision);

    /**
     * Find all denied access attempts for a specific user
     */
    List<AuthorizationAudit> findByUserIdAndDecisionOrderByCreatedAtDesc(Long userId, String decision);

    /**
     * Find audit logs by endpoint key
     */
    List<AuthorizationAudit> findByEndpointKeyOrderByCreatedAtDesc(String endpointKey);

    /**
     * Find audit logs for a specific endpoint and user
     */
    List<AuthorizationAudit> findByEndpointKeyAndUserIdOrderByCreatedAtDesc(String endpointKey, Long userId);

    /**
     * Find recent audit logs (last N records)
     */
    List<AuthorizationAudit> findTop100ByOrderByCreatedAtDesc();

    /**
     * Find audit logs within a date range
     */
    List<AuthorizationAudit> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count denied access attempts for a user
     */
    long countByUserIdAndDecision(Long userId, String decision);

    /**
     * Count denied access attempts for an endpoint
     */
    long countByEndpointKeyAndDecision(String endpointKey, String decision);

    /**
     * Find denied access attempts by IP address (for security monitoring)
     */
    List<AuthorizationAudit> findByIpAddressAndDecisionOrderByCreatedAtDesc(String ipAddress, String decision);

    /**
     * Count denied access attempts from an IP address
     */
    long countByIpAddressAndDecision(String ipAddress, String decision);

    /**
     * Get access statistics for a user
     */
    @Query("SELECT a.decision, COUNT(a) FROM AuthorizationAudit a " +
           "WHERE a.userId = :userId " +
           "AND a.createdAt >= :since " +
           "GROUP BY a.decision")
    List<Object[]> getUserAccessStatistics(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Get access statistics for an endpoint
     */
    @Query("SELECT a.decision, COUNT(a) FROM AuthorizationAudit a " +
           "WHERE a.endpointKey = :endpointKey " +
           "AND a.createdAt >= :since " +
           "GROUP BY a.decision")
    List<Object[]> getEndpointAccessStatistics(@Param("endpointKey") String endpointKey, @Param("since") LocalDateTime since);

    /**
     * Find suspicious activity (multiple denied attempts from same IP)
     */
    @Query("SELECT a.ipAddress, COUNT(a) as deniedCount FROM AuthorizationAudit a " +
           "WHERE a.decision = 'DENIED' " +
           "AND a.createdAt >= :since " +
           "GROUP BY a.ipAddress " +
           "HAVING COUNT(a) > :threshold " +
           "ORDER BY deniedCount DESC")
    List<Object[]> findSuspiciousIPs(@Param("since") LocalDateTime since, @Param("threshold") long threshold);
}
