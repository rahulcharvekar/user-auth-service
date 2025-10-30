package com.example.userauth.entity;

import jakarta.persistence.*;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shared.entityaudit.annotation.EntityAuditEnabled;
import com.shared.entityaudit.descriptor.AbstractAuditableEntity;
import com.shared.entityaudit.listener.SharedEntityAuditListener;

/**
 * Junction table linking Endpoints to Policies (Many-to-Many relationship).
 * Defines which policies protect which endpoints.
 */
@Entity
@EntityAuditEnabled
@EntityListeners(SharedEntityAuditListener.class)
@Table(name = "endpoint_policies")
public class EndpointPolicy extends AbstractAuditableEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    public EndpointPolicy() {
    }

    public EndpointPolicy(Endpoint endpoint, Policy policy) {
        this.endpoint = endpoint;
        this.policy = policy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    @Override
    public String entityType() {
        return "ENDPOINT_POLICY";
    }

    @Override
    public Map<String, Object> auditState() {
        return auditStateOf(
                "id", id,
                "endpointId", endpoint != null ? endpoint.getId() : null,
                "policyId", policy != null ? policy.getId() : null
        );
    }

    @Override
    public String toString() {
        return "EndpointPolicy{" +
                "id=" + id +
                ", endpointId=" + (endpoint != null ? endpoint.getId() : null) +
                ", policyId=" + (policy != null ? policy.getId() : null) +
                '}';
    }
}
