package com.example.userauth.repository;

import com.example.userauth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    // Only keep methods needed for WRITE operations and entity lookups during writes
    
    @Query("SELECT r FROM Role r WHERE r.name IN :names")
    List<Role> findByNames(@Param("names") List<String> names);

    Optional<Role> findByName(String name);
    
    // Keep for write operation validations
    boolean existsByName(String name);

    Optional<Role> findTopByOrderByIdDesc();
}
