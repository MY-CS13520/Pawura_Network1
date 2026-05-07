package com.pawura;

import com.pawura.core.AbstractDataStore;
import com.pawura.model.ActivityLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActivityLogStore extends AbstractDataStore<ActivityLog, Integer> {

    @Override
    public boolean save(ActivityLog entity) {
        String sql = "INSERT INTO activity_logs (admin_id, action, target_type, created_at) VALUES (?, ?, ?, ?)";
        return executeUpdate(sql, entity.getAdminId(), entity.getAction(), entity.getTargetType(), Timestamp.valueOf(entity.getCreatedAt()));
    }

    @Override public boolean update(ActivityLog entity) { return false; }
    @Override public boolean delete(Integer id) { return false; }
    @Override public Optional<ActivityLog> findById(Integer id) { return Optional.empty(); }

    @Override
    public List<ActivityLog> findAll() {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = """
            SELECT a.*, u.username as admin_name 
            FROM activity_logs a 
            LEFT JOIN users u ON a.admin_id = u.id 
            ORDER BY a.created_at DESC 
            LIMIT 100""";
        
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.severe("Error fetching activity logs: " + e.getMessage());
        }
        return logs;
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getInt("id"));
        log.setAdminId(rs.getInt("admin_id"));
        log.setAdminName(rs.getString("admin_name"));
        log.setAction(rs.getString("action"));
        log.setTargetType(rs.getString("target_type"));
        log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return log;
    }

    @Override protected String entityName() { return "ActivityLog"; }
}