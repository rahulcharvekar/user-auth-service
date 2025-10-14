package com.example.userauth.entity;

import jakarta.persistence.*;

/**
 * Junction table linking Endpoints to Policies (Many-to-Many relationship).
 * Defines which policies protect which endpoints.
 */
@Entity
@Table(name = "endpoint_policies")
public class EndpointPolicy {

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
    public String toString() {
        return "EndpointPolicy{" +
                "id=" + id +
                ", endpointId=" + (endpoint != null ? endpoint.getId() : null) +
                ", policyId=" + (policy != null ? policy.getId() : null) +
                '}';
    }
}
