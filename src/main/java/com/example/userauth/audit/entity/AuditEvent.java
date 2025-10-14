package com.example.userauth.audit.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;
    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;
    @Column(name = "action", nullable = false, length = 128)
    private String action;
    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;
    @Column(name = "resource_id", length = 128)
    private String resourceId;
    @Column(name = "outcome", nullable = false, length = 16)
    private String outcome;
    @Column(name = "client_ip", length = 64)
    private String clientIp;
    @Column(name = "user_agent", length = 256)
    private String userAgent;
    @Column(name = "details", columnDefinition = "json")
    private String details;
    @Column(name = "referer", length = 256)
    private String referer;
    @Column(name = "client_source", length = 64)
    private String clientSource;
    @Column(name = "requested_with", length = 64)
    private String requestedWith;
    @Column(name = "old_values", columnDefinition = "json")
    private String oldValues;
    @Column(name = "new_values", columnDefinition = "json")
    private String newValues;
    @Column(name = "prev_hash", nullable = false, length = 64)
    private String prevHash;
    @Column(name = "hash", nullable = false, length = 64)
    private String hash;
    @Column(name = "response_hash", length = 64)
    private String responseHash;
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
    public String getClientSource() { return clientSource; }
    public void setClientSource(String clientSource) { this.clientSource = clientSource; }
    public String getRequestedWith() { return requestedWith; }
    public void setRequestedWith(String requestedWith) { this.requestedWith = requestedWith; }
    public String getOldValues() { return oldValues; }
    public void setOldValues(String oldValues) { this.oldValues = oldValues; }
    public String getNewValues() { return newValues; }
    public void setNewValues(String newValues) { this.newValues = newValues; }
    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public String getResponseHash() { return responseHash; }
    public void setResponseHash(String responseHash) { this.responseHash = responseHash; }
}
