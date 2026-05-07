package com.pawura;

import com.pawura.core.AbstractDataStore;
import com.pawura.model.Administrator;
import com.pawura.model.User;
import java.util.ArrayList;
import java.sql.*;
import java.util.List;
import java.util.Optional;

public class UserStore extends AbstractDataStore<User, Integer> {

    @Override
    public boolean save(User entity) { return false; } // Handled by Auth Service

    @Override
    public boolean update(User entity) {
        String sql = "UPDATE users SET role = ?, active = ?, full_name = ? WHERE id = ?";
        return executeUpdate(sql, entity.getRole().name(), entity.isActive(), entity.getFullName(), entity.getId());
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
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
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

    private User mapRow(ResultSet rs) throws SQLException {
        User.Role role = User.Role.valueOf(rs.getString("role").trim().toUpperCase());
        User user = (role == User.Role.ADMINISTRATOR) ? new Administrator() : new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(role);
        user.setActive(rs.getBoolean("active"));
        user.setVerified(rs.getBoolean("is_verified"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) user.setCreatedAt(ts.toLocalDateTime());
        return user;
    }

    @Override
    protected String entityName() { return "User"; }
}