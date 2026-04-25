package com.pawura.model;

import java.time.LocalDateTime;

/**
 * User – base model demonstrating ENCAPSULATION.
 * All fields are private; access is through validated getters/setters.
 */
public class User {

    public enum Role { VIEWER, RANGER, ADMINISTRATOR }

    // ── Private Fields ────────────────────────────────────────────────────────
    private int           id;
    private String        username;
    private String        passwordHash;   // BCrypt hash – never the raw password
    private String        email;
    private String        fullName;
    private Role          role;
    private LocalDateTime createdAt;
    private boolean       isVerified;
    private String        otpCode;
    private LocalDateTime otpExpiry;
    private boolean       active;

    // ── Constructors ──────────────────────────────────────────────────────────
    public User() {
        this.role      = Role.VIEWER;
        this.active    = true;
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String passwordHash, String email,
                String fullName, Role role) {
        this();
        this.username     = username;
        this.passwordHash = passwordHash;
        this.email        = email;
        this.fullName     = fullName;
        this.role         = role;
    }

    // ── Getters & Setters (Encapsulation) ─────────────────────────────────────
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public String getUsername()               { return username; }
    public void setUsername(String username) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username cannot be blank.");
        this.username = username.trim();
    }

    public String getPasswordHash()           { return passwordHash; }
    public void setPasswordHash(String hash)  { this.passwordHash = hash; }

    public String getEmail()                  { return email; }
    public void setEmail(String email) {
        if (email == null || !email.contains("@"))
            throw new IllegalArgumentException("Invalid email address.");
        this.email = email;
    }

    public String getFullName()               { return fullName; }
    public void setFullName(String fullName)  { this.fullName = fullName; }

    public Role getRole()                     { return role; }
    public void setRole(Role role)            { this.role = role; }

    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime d) { this.createdAt = d; }

    public boolean isVerified()               { return isVerified; }
    public void setVerified(boolean verified) { this.isVerified = verified; }

    public String getOtpCode()                { return otpCode; }
    public void setOtpCode(String otpCode)    { this.otpCode = otpCode; }

    public LocalDateTime getOtpExpiry()       { return otpExpiry; }
    public void setOtpExpiry(LocalDateTime d) { this.otpExpiry = d; }

    public boolean isActive()                 { return active; }
    public void setActive(boolean active)     { this.active = active; }

    public boolean isAdmin() { return role == Role.ADMINISTRATOR; }

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', role=%s}", id, username, role);
    }
}
