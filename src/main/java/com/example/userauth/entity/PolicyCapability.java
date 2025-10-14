package com.example.userauth.entity;

import jakarta.persistence.*;

/**
 * Junction table linking Policies to Capabilities (Many-to-Many relationship).
 * Defines which capabilities are granted by which policies.
 */
@Entity
@Table(name = "policy_capabilities")
public class PolicyCapability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "capability_id", nullable = false)
    private Capability capability;

    public PolicyCapability() {
    }

    public PolicyCapability(Policy policy, Capability capability) {
        this.policy = policy;
        this.capability = capability;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public Capability getCapability() {
        return capability;
    }

    public void setCapability(Capability capability) {
        this.capability = capability;
    }

    @Override
    public String toString() {
        return "PolicyCapability{" +
                "id=" + id +
                ", policyId=" + (policy != null ? policy.getId() : null) +
                ", capabilityId=" + (capability != null ? capability.getId() : null) +
                '}';
    }
}
