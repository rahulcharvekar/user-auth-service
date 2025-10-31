package com.example.userauth.repository;

import com.example.userauth.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Policy entity.
 * Provides methods to query policies by various criteria.
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    /**
     * Find a policy by its unique name
     */
    Optional<Policy> findByName(String name);

    /**
     * Find all policies of a given type (RBAC, ABAC, CUSTOM)
     */
    List<Policy> findByType(String type);

    /**
     * Find all active policies
     */
    List<Policy> findByIsActiveTrue();

    /**
     * Find active policies by type
     */
    List<Policy> findByTypeAndIsActiveTrue(String type);

    /**
     * Check if a policy exists by name
     */
    boolean existsByName(String name);

    /**
     * Find RBAC policies that apply to a specific role
     * This searches the JSON expression for the role name
     */
    @Query("SELECT p FROM Policy p " +
           "WHERE p.type = 'RBAC' " +
           "AND p.expression LIKE CONCAT('%', :roleName, '%') " +
           "AND p.isActive = true")
    List<Policy> findRBACPoliciesByRole(@Param("roleName") String roleName);

    /**
     * Find policies that grant a specific capability
     */
    @Query("SELECT DISTINCT p FROM Policy p " +
           "JOIN PolicyCapability pc ON pc.policy.id = p.id " +
           "JOIN Capability c ON c.id = pc.capability.id " +
           "WHERE c.name = :capabilityName " +
           "AND p.isActive = true")
    List<Policy> findPoliciesByCapabilityName(@Param("capabilityName") String capabilityName);

    /**
     * Get all capabilities granted by a specific policy
     */
    @Query("SELECT c.name FROM Capability c " +
           "JOIN PolicyCapability pc ON pc.capability.id = c.id " +
           "WHERE pc.policy.id = :policyId " +
           "AND c.isActive = true")
    List<String> findCapabilityNamesByPolicyId(@Param("policyId") Long policyId);
    
    /**
     * Find policies linked to a specific endpoint
     */
    @Query("SELECT p FROM Policy p " +
           "JOIN EndpointPolicy ep ON ep.policy.id = p.id " +
           "WHERE ep.endpoint.id = :endpointId " +
           "AND p.isActive = true")
    List<Policy> findByEndpointId(@Param("endpointId") Long endpointId);

    Optional<Policy> findTopByOrderByIdDesc();
}
