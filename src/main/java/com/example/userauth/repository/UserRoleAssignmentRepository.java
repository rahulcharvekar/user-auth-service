package com.example.userauth.repository;

import com.example.userauth.entity.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for UserRoleAssignment entity.
 * Provides methods to query user-role assignments.
 */
@Repository
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, Long> {

    /**
     * Find all role assignments for a specific user
     */
    List<UserRoleAssignment> findByUserId(Long userId);

    /**
     * Find all users with a specific role
     */
    List<UserRoleAssignment> findByRoleId(Long roleId);

    /**
     * Find all users with a specific role (by role name)
     */
    @Query("SELECT ura FROM UserRoleAssignment ura " +
           "JOIN Role r ON ura.role.id = r.id " +
           "WHERE r.name = :roleName")
    List<UserRoleAssignment> findByRoleName(@Param("roleName") String roleName);

    /**
     * Check if a user has a specific role
     */
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * Check if a user has a specific role (by role name)
     */
    @Query("SELECT CASE WHEN COUNT(ura) > 0 THEN true ELSE false END " +
           "FROM UserRoleAssignment ura " +
           "JOIN Role r ON ura.role.id = r.id " +
           "WHERE ura.user.id = :userId " +
           "AND r.name = :roleName")
    boolean userHasRole(@Param("userId") Long userId, @Param("roleName") String roleName);

    /**
     * Get all role names for a specific user
     */
    @Query("SELECT r.name FROM UserRoleAssignment ura " +
           "JOIN Role r ON ura.role.id = r.id " +
           "WHERE ura.user.id = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    /**
     * Delete all role assignments for a user
     */
    void deleteByUserId(Long userId);

    /**
     * Delete a specific user-role assignment
     */
    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * Count role assignments for a user
     */
    long countByUserId(Long userId);

    /**
     * Count users with a specific role
     */
    long countByRoleId(Long roleId);
}
