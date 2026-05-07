package com.pawura;

import com.pawura.core.AbstractDataStore;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SightingStore extends AbstractDataStore<Sighting, Integer> {

    @Override public boolean save(Sighting entity) { return false; }

    @Override
    public boolean update(Sighting entity) {
        String sql = "UPDATE sightings SET verified = ?, elephant_count = ?, notes = ? WHERE id = ?";
        return executeUpdate(sql, entity.isVerified(), entity.getElephantCount(), entity.getNotes(), entity.getId());
    }

    @Override
    public boolean delete(Integer id) {
        return executeUpdate("DELETE FROM sightings WHERE id = ?", id);
    }

    @Override
    public Optional<Sighting> findById(Integer id) {
        String sql = "SELECT * FROM sightings WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) { log.severe(e.getMessage()); }
        return Optional.empty();
    }

    @Override
    public List<Sighting> findAll() {
        List<Sighting> list = new ArrayList<>();
        try (ResultSet rs = getConnection().createStatement().executeQuery("SELECT * FROM sightings ORDER BY sighted_at DESC")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { log.severe(e.getMessage()); }
        return list;
    }

    public long countUnverified() {
        try (ResultSet rs = getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM sightings WHERE verified = 0")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException ignored) {}
        return 0;
    }

    private Sighting mapRow(ResultSet rs) throws SQLException {
        Sighting s = new Sighting();
        s.setId(rs.getInt("id"));
        s.setLocationId(rs.getInt("location_id"));
        s.setReportedBy(rs.getInt("reported_by"));
        s.setSightedAt(rs.getTimestamp("sighted_at").toLocalDateTime());
        s.setElephantCount(rs.getInt("elephant_count"));
        s.setHerdSize(rs.getString("herd_size"));
        s.setBehaviour(rs.getString("behaviour"));
        s.setNotes(rs.getString("notes"));
        s.setVerified(rs.getBoolean("verified"));
        return s;
    }

    @Override protected String entityName() { return "Sighting"; }
}