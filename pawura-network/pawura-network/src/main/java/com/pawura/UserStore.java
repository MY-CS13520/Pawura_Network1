package com.pawura;

import com.pawura.core.AbstractDataStore;
import com.pawura.model.Administrator;
import com.pawura.model.User;
import java.util.ArrayList;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

public class UserStore extends AbstractDataStore<User, Integer> {

    @Override
    public boolean save(User entity) { return false; } // Handled by Auth Service

    @Override
    public boolean update(User entity) {
        String sql = "UPDATE users SET role = ?, active = ?, full_name = ?, home_lat = ?, home_lon = ?, notification_radius = ? WHERE id = ?";
        return executeUpdate(sql, entity.getRole().name(), entity.isActive(), entity.getFullName(), entity.getHomeLat(), entity.getHomeLon(), entity.getNotificationRadius(), entity.getId());
    }

    @Override
    public boolean delete(Integer id) {
        return executeUpdate("DELETE FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            log.severe("Error finding user: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        // Explicitly selecting columns to ensure the result set matches mapRow's expectations
        String sql = "SELECT id, username, email, full_name, home_lat, home_lon, notification_radius, role, active, is_verified, created_at " +
                     "FROM users ORDER BY created_at DESC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            log.severe("Error fetching users: " + e.getMessage());
        }
        return users;
    }

    public long countActiveUsers() {
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE active = 1")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException ignored) {}
        return 0;
    }

    public Map<String, Integer> getRegistrationTrends() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String sql = "SELECT DATE(created_at) as reg_date, COUNT(*) as count FROM users GROUP BY reg_date ORDER BY reg_date ASC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("reg_date"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            log.severe("Error fetching registration trends: " + e.getMessage());
        }
        return stats;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        // Access columns in sequential order (id, username, email, full_name, home_lat, home_lon, notification_radius, role, active, is_verified, created_at)
        // to ensure maximum compatibility with JDBC result sets.
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String fullName = rs.getString("full_name");
        double lat = rs.getDouble("home_lat");
        double lon = rs.getDouble("home_lon");
        double radius = rs.getDouble("notification_radius");
        String roleStr = rs.getString("role");
        boolean active = rs.getBoolean("active");
        boolean verified = rs.getBoolean("is_verified");
        Timestamp ts = rs.getTimestamp("created_at");

        User.Role role = User.Role.VIEWER;
        if (roleStr != null) {
            try {
                role = User.Role.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warning("Unknown role '" + roleStr + "' for user, defaulting to VIEWER");
            }
        }

        User user = (role == User.Role.ADMINISTRATOR) ? new Administrator() : new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(active);
        user.setVerified(verified);
        user.setHomeLat(lat);
        user.setHomeLon(lon);
        user.setNotificationRadius(radius);
        if (ts != null) user.setCreatedAt(ts.toLocalDateTime());
        return user;
    }

    @Override
    protected String entityName() { return "User"; }
}