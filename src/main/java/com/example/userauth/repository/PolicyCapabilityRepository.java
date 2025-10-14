package com.example.userauth.repository;

import com.example.userauth.entity.PolicyCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PolicyCapability entity.
 * Manages the many-to-many relationship between policies and capabilities.
 */
@Repository
public interface PolicyCapabilityRepository extends JpaRepository<PolicyCapability, Long> {

    /**
     * Find all policy-capability associations for a policy
     */
    List<PolicyCapability> findByPolicyId(Long policyId);

    /**
     * Find all policy-capability associations for a capability
     */
    List<PolicyCapability> findByCapabilityId(Long capabilityId);

    /**
     * Check if a policy-capability association exists
     */
    boolean existsByPolicyIdAndCapabilityId(Long policyId, Long capabilityId);

    /**
     * Delete all policy-capability associations for a policy
     */
    @Modifying
    @Query("DELETE FROM PolicyCapability pc WHERE pc.policy.id = :policyId")
    void deleteByPolicyId(@Param("policyId") Long policyId);

    /**
     * Delete all policy-capability associations for a capability
     */
    @Modifying
    @Query("DELETE FROM PolicyCapability pc WHERE pc.capability.id = :capabilityId")
    void deleteByCapabilityId(@Param("capabilityId") Long capabilityId);

    /**
     * Delete specific policy-capability association
     */
    @Modifying
    @Query("DELETE FROM PolicyCapability pc WHERE pc.policy.id = :policyId AND pc.capability.id = :capabilityId")
    void deleteByPolicyIdAndCapabilityId(@Param("policyId") Long policyId, @Param("capabilityId") Long capabilityId);
}
