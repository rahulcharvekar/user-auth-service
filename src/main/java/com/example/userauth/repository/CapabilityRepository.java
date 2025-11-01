package com.example.userauth.repository;

import com.example.userauth.entity.Capability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Capability entity.
 * Provides methods to query capabilities by various criteria.
 */
@Repository
public interface CapabilityRepository extends JpaRepository<Capability, Long> {

    /**
     * Find a capability by its unique name
     */
    Optional<Capability> findByName(String name);

    /**
     * Find all capabilities for a given module
     */
    List<Capability> findByModule(String module);

    /**
     * Find capabilities by module and action
     */
    List<Capability> findByModuleAndAction(String module, String action);

    /**
     * Find all active capabilities
     */
    List<Capability> findByIsActiveTrue();

    /**
     * Find all active capabilities for a module
     */
    List<Capability> findByModuleAndIsActiveTrue(String module);

    /**
     * Find capabilities by resource
     */
    List<Capability> findByResource(String resource);

    /**
     * Check if a capability exists by name
     */
    boolean existsByName(String name);

    /**
     * Find capabilities by multiple names (for batch queries)
     */
    List<Capability> findByNameIn(List<String> names);

    /**
     * Get all capability names for a user based on their roles
     * This joins through user_roles -> roles -> policies -> policy_capabilities -> capabilities
     */
    @Query("SELECT DISTINCT c.name FROM Capability c " +
           "JOIN PolicyCapability pc ON pc.capability.id = c.id " +
           "JOIN Policy p ON p.id = pc.policy.id " +
           "WHERE p.isActive = true " +
           "AND c.isActive = true")
    List<String> findAllCapabilityNames();

    /**
     * Find capabilities granted to a specific role through policies
     */
    @Query("SELECT DISTINCT c FROM Capability c " +
           "JOIN PolicyCapability pc ON pc.capability.id = c.id " +
           "JOIN Policy p ON p.id = pc.policy.id " +
           "WHERE CAST(p.expression AS string) LIKE CONCAT('%', :roleName, '%') " +
           "AND p.isActive = true " +
           "AND c.isActive = true")
    List<Capability> findByRoleName(@Param("roleName") String roleName);
    
    /**
     * Find capability names granted to a specific role through policies
     */
    @Query("SELECT DISTINCT c.name FROM Capability c " +
           "JOIN PolicyCapability pc ON pc.capability.id = c.id " +
           "JOIN Policy p ON p.id = pc.policy.id " +
           "WHERE CAST(p.expression AS string) LIKE CONCAT('%', :roleName, '%') " +
           "AND p.isActive = true " +
           "AND c.isActive = true")
    List<String> findCapabilityNamesByRoleName(@Param("roleName") String roleName);
}
