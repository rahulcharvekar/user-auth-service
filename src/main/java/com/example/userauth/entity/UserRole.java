package com.example.userauth.entity;

public enum UserRole {
    ADMIN("Administrator"),
    USER("User"),
    WORKER("Worker"),
    BOARD("Board Member"),
    EMPLOYER("Employer"),
    RECONCILIATION_OFFICER("Reconciliation Officer");
    
    private final String displayName;
    
    UserRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
