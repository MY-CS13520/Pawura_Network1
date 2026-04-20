package com.pawura.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Administrator – extends User, demonstrating INHERITANCE.
 * Adds admin-specific capabilities: activity log and permission management.
 */
public class Administrator extends User {

    private String        department;
    private List<String>  activityLog;
    private LocalDateTime lastLoginAt;

    // ── Constructors ──────────────────────────────────────────────────────────
    public Administrator() {
        super();
        setRole(Role.ADMINISTRATOR);
        this.activityLog = new ArrayList<>();
    }

    public Administrator(String username, String passwordHash,
                         String email, String fullName, String department) {
        super(username, passwordHash, email, fullName, Role.ADMINISTRATOR);
        this.department  = department;
        this.activityLog = new ArrayList<>();
    }

    // ── Admin-specific behaviour ──────────────────────────────────────────────

    /** Log an action performed by this administrator. */
    public void logActivity(String action) {
        activityLog.add(LocalDateTime.now() + " – " + action);
    }

    /** @return unmodifiable view of the activity log */
    public List<String> getActivityLog() {
        return List.copyOf(activityLog);
    }

    /** Deactivate a regular user account. */
    public void deactivateUser(User user) {
        if (user instanceof Administrator)
            throw new IllegalArgumentException("Cannot deactivate another administrator.");
        user.setActive(false);
        logActivity("Deactivated user: " + user.getUsername());
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public String getDepartment()                   { return department; }
    public void setDepartment(String department)    { this.department = department; }

    public LocalDateTime getLastLoginAt()           { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime t)     { this.lastLoginAt = t; }

    @Override
    public String toString() {
        return String.format("Administrator{id=%d, username='%s', dept='%s'}",
                             getId(), getUsername(), department);
    }
}
