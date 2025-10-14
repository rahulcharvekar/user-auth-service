package com.example.userauth.repository;

/**
 * DEPRECATED: Repository for old Permission system
 * The new system uses CapabilityRepository and PolicyRepository
 * This interface is disabled (not extending JpaRepository) to prevent Spring from creating a bean
 * 
 * @deprecated Use {@link CapabilityRepository} and {@link PolicyRepository} instead
 */
@Deprecated
// @Repository - REMOVED to prevent Spring from creating bean
// Stub interface to satisfy imports - does NOT extend JpaRepository
public interface PermissionRepository {
    // OLD INTERFACE - DO NOT USE
    // This is just a stub to satisfy compilation, Spring will not create a bean for this
}
