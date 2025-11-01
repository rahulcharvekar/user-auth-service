package com.example.userauth.repository;

import com.example.userauth.entity.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Endpoint entity.
 * Provides methods to query service catalog endpoints.
 */
@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

       /**
        * Find all endpoints by a list of IDs
        */
       List<Endpoint> findByIdIn(List<Long> ids);

    /**
     * Find an endpoint by service, version, method and path
     */
    Optional<Endpoint> findByServiceAndVersionAndMethodAndPath(String service, String version, String method, String path);

    /**
     * Find all endpoints for a given service
     */
    List<Endpoint> findByService(String service);

    /**
     * Find all active endpoints
     */
    List<Endpoint> findByIsActiveTrue();

    /**
     * Find all active endpoints for a service
     */
    List<Endpoint> findByServiceAndIsActiveTrue(String service);

    /**
     * Find all endpoints protected by a specific policy
     */
    @Query("SELECT DISTINCT e FROM Endpoint e " +
           "JOIN EndpointPolicy ep ON ep.endpoint.id = e.id " +
           "JOIN Policy p ON p.id = ep.policy.id " +
           "WHERE p.name = :policyName " +
           "AND e.isActive = true")
    List<Endpoint> findByPolicyName(@Param("policyName") String policyName);

    /**
     * Find all endpoints accessible to a role
     * This joins through policies that apply to the role
     */
    @Query("SELECT DISTINCT e FROM Endpoint e " +
           "JOIN EndpointPolicy ep ON ep.endpoint.id = e.id " +
           "JOIN Policy p ON p.id = ep.policy.id " +
           "WHERE CAST(p.expression AS string) LIKE CONCAT('%', :roleName, '%') " +
           "AND p.isActive = true " +
           "AND e.isActive = true")
    List<Endpoint> findAccessibleByRole(@Param("roleName") String roleName);

    /**
     * Find endpoints by HTTP method
     */
    List<Endpoint> findByMethod(String method);
    
    /**
     * Find all active endpoints ordered by service and version
     */
    List<Endpoint> findByIsActiveTrueOrderByServiceAscVersionAsc();
}
