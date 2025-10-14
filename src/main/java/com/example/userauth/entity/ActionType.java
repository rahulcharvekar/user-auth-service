package com.example.userauth.entity;

public enum ActionType {
    // Basic CRUD operations
    VIEW("View", "Can view/read the component"),
    CREATE("Create", "Can create new records in the component"),
    EDIT("Edit", "Can modify existing records in the component"),
    DELETE("Delete", "Can delete records from the component"),
    
    // Workflow operations
    APPROVE("Approve", "Can approve records/transactions"),
    REJECT("Reject", "Can reject records/transactions"),
    SUBMIT("Submit", "Can submit records for processing"),
    
    // File operations
    UPLOAD("Upload", "Can upload files to the component"),
    DOWNLOAD("Download", "Can download files from the component"),
    EXPORT("Export", "Can export data from the component"),
    
    // Administrative operations
    CONFIGURE("Configure", "Can configure component settings"),
    MANAGE("Manage", "Full management access to the component"),
    
    // Reporting operations
    REPORT("Report", "Can generate reports from the component"),
    ANALYTICS("Analytics", "Can view analytics and insights");
    
    private final String displayName;
    private final String description;
    
    ActionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
