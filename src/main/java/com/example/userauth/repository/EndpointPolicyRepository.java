package com.example.userauth.repository;

import com.example.userauth.entity.EndpointPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for EndpointPolicy entity.
 * Manages the many-to-many relationship between endpoints and policies.
 */
@Repository
public interface EndpointPolicyRepository extends JpaRepository<EndpointPolicy, Long> {
    /**
     * Find all endpoint-policy associations for a list of policy IDs
     */
    List<EndpointPolicy> findByPolicyIdIn(List<Long> policyIds);

    /**
     * Find all endpoint-policy associations for an endpoint
     */
    List<EndpointPolicy> findByEndpointId(Long endpointId);

    /**
     * Find all endpoint-policy associations for a policy
     */
    List<EndpointPolicy> findByPolicyId(Long policyId);

    /**
     * Check if an endpoint-policy association exists
     */
    boolean existsByEndpointIdAndPolicyId(Long endpointId, Long policyId);

    /**
     * Delete all endpoint-policy associations for an endpoint
     */
    @Modifying
    @Query("DELETE FROM EndpointPolicy ep WHERE ep.endpoint.id = :endpointId")
    void deleteByEndpointId(@Param("endpointId") Long endpointId);

    /**
     * Delete all endpoint-policy associations for a policy
     */
    @Modifying
    @Query("DELETE FROM EndpointPolicy ep WHERE ep.policy.id = :policyId")
    void deleteByPolicyId(@Param("policyId") Long policyId);

    /**
     * Delete specific endpoint-policy association
     */
    @Modifying
    @Query("DELETE FROM EndpointPolicy ep WHERE ep.endpoint.id = :endpointId AND ep.policy.id = :policyId")
    void deleteByEndpointIdAndPolicyId(@Param("endpointId") Long endpointId, @Param("policyId") Long policyId);
}
