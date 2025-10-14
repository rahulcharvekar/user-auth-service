package com.example.userauth.repository;

import com.example.userauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Keep methods needed for authentication and write operations
    
    // Used by Spring Security for authentication
    Optional<User> findByUsername(String username);
    
    // Keep for write operation validations
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    // Used for login functionality - keep for authentication
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
}
