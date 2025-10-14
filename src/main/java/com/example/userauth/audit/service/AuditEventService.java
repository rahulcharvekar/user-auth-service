package com.example.userauth.audit.service;

import com.example.userauth.audit.entity.AuditEvent;
import com.example.userauth.audit.repository.AuditEventRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
public class AuditEventService {
    private final AuditEventRepository auditEventRepository;
    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }
    @Async
    public CompletableFuture<AuditEvent> recordEvent(AuditEvent event, String prevHash) {
        event.setOccurredAt(Instant.now());
        event.setPrevHash(prevHash != null ? prevHash : "0");
        event.setHash(computeHash(event, event.getPrevHash()));
        AuditEvent saved = auditEventRepository.save(event);
        return CompletableFuture.completedFuture(saved);
    }
    private String computeHash(AuditEvent event, String prevHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = prevHash +
                    "," + event.getOccurredAt() +
                    "," + event.getTraceId() +
                    "," + event.getUserId() +
                    "," + event.getAction() +
                    "," + event.getResourceType() +
                    "," + event.getResourceId() +
                    "," + event.getOutcome() +
                    "," + event.getClientIp() +
                    "," + event.getUserAgent() +
                    "," + event.getDetails() +
                    "," + event.getOldValues() +
                    "," + event.getNewValues();
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
