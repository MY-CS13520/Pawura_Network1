package com.pawura.service;

import com.pawura.database.DatabaseManager;
import com.pawura.model.Administrator;
import com.pawura.model.User;
import com.pawura.util.PasswordUtils;
import com.pawura.util.ValidationUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * AuthenticationService – login, register, and session management.
 */
public class AuthenticationService {

    private static final Logger LOG = Logger.getLogger(AuthenticationService.class.getName());

    private User currentUser;

    // ── Login / Logout ────────────────────────────────────────────────────────

    public Optional<User> login(String username, String password) {
        ValidationUtils.requireNonBlank(username, "Username");
        ValidationUtils.requireNonBlank(password, "Password");

        String sql = "SELECT * FROM users WHERE username = ? AND active = 1";
        try (PreparedStatement ps =
                DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (PasswordUtils.verify(password, storedHash)) {
                    User user = mapRow(rs);
                    currentUser = user;
                    LOG.info("Login OK: " + username);
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            LOG.severe("Login error: " + e.getMessage());
        }
        LOG.warning("Login failed for: " + username);
        return Optional.empty();
    }

    public void logout() {
        LOG.info("User logged out: " +
                 (currentUser != null ? currentUser.getUsername() : "none"));
        currentUser = null;
    }

    public Optional<User> getCurrentUser()    { return Optional.ofNullable(currentUser); }
    public boolean isLoggedIn()               { return currentUser != null; }

    // ── Registration ──────────────────────────────────────────────────────────

    public boolean register(String username, String password,
                            String email, String fullName, User.Role role) {
        if (!ValidationUtils.isValidUsername(username))
            throw new IllegalArgumentException("Username must be 3-30 alphanumeric characters.");
        if (!ValidationUtils.isValidPassword(password))
            throw new IllegalArgumentException("Password must be at least 6 characters.");

        String hash = PasswordUtils.hash(password);
        String sql  = """
            INSERT INTO users(username,password_hash,email,full_name,role,active,created_at)
            VALUES(?,?,?,?,?,1,?)""";

        try (PreparedStatement ps =
                DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, email);
            ps.setString(4, fullName);
            ps.setString(5, role.name());
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
            LOG.info("Registered new user: " + username);
            return true;
        } catch (SQLException e) {
            LOG.warning("Registration failed for " + username + ": " + e.getMessage());
            return false;
        }
    }

    // ── Row Mapper ────────────────────────────────────────────────────────────

    private User mapRow(ResultSet rs) throws SQLException {
        User.Role role = User.Role.valueOf(rs.getString("role"));
        User user = role == User.Role.ADMINISTRATOR ? new Administrator() : new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(role);
        user.setActive(rs.getInt("active") == 1);
        String createdAt = rs.getString("created_at");
        if (createdAt != null)
            user.setCreatedAt(LocalDateTime.parse(createdAt));
        return user;
    }
}
