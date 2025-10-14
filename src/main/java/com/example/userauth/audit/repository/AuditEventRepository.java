package com.example.userauth.audit.repository;

import com.example.userauth.audit.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    // Custom query methods can be added here if needed
}
