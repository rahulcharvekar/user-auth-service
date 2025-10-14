package com.example.userauth.repository;

/**
 * DEPRECATED: Repository for old Permission API Endpoint system
 * The new system uses EndpointRepository with Endpoint+EndpointPolicy architecture
 * This interface is disabled (not extending JpaRepository) to prevent Spring from creating a bean
 * 
 * @deprecated Use {@link EndpointRepository} instead
 */
@Deprecated
// Stub interface to satisfy imports - does NOT extend JpaRepository to prevent Spring bean creation
public interface PermissionApiEndpointRepository {
    // OLD INTERFACE - DO NOT USE
    // This is just a stub to satisfy compilation, Spring will not create a bean for this
}
