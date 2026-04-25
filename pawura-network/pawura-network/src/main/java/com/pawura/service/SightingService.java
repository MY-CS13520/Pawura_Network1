package com.pawura.service;

import com.pawura.database.DatabaseManager;
import com.pawura.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * SightingService – save, fetch, and verify elephant sightings.
 */
public class SightingService {

    private static final Logger LOG = Logger.getLogger(SightingService.class.getName());

    // ── Save ──────────────────────────────────────────────────────────────────

    public boolean save(ElephantSighting s) {
        String sql = """
            INSERT INTO sightings
              (location_id, reported_by, sighted_at, elephant_count,
               herd_size, behaviour, notes, verified)
            VALUES (?,?,?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, s.getLocation()    != null ? s.getLocation().getId()    : null);
            ps.setObject(2, s.getReportedBy()  != null ? s.getReportedBy().getId()  : null);
            ps.setTimestamp(3, Timestamp.valueOf(s.getSightedAt()));
            ps.setInt   (4, s.getElephantCount());
            ps.setString(5, s.getHerdSize()   != null ? s.getHerdSize().name()   : null);
            ps.setString(6, s.getBehaviour()  != null ? s.getBehaviour().name()  : null);
            ps.setString(7, s.getNotes());
            ps.setBoolean(8, s.isVerified());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
            LOG.info("Saved sighting id=" + s.getId());
            return true;
        } catch (SQLException e) {
            LOG.severe("Save sighting error: " + e.getMessage());
            return false;
        }
    }

    // ── Fetch All ─────────────────────────────────────────────────────────────

    public List<ElephantSighting> findAll() {
        List<ElephantSighting> list = new ArrayList<>();
        String sql = """
            SELECT s.*, l.name loc_name, l.district, l.latitude, l.longitude,
                   u.full_name reporter_name
            FROM sightings s
            LEFT JOIN locations l ON l.id = s.location_id
            LEFT JOIN users u     ON u.id = s.reported_by
            ORDER BY s.sighted_at DESC""";
        try (ResultSet rs = conn().createStatement().executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOG.severe("FindAll sightings error: " + e.getMessage());
        }
        return list;
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    public boolean verify(int sightingId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE sightings SET verified=1 WHERE id=?")) {
            ps.setInt(1, sightingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.severe("Verify sighting error: " + e.getMessage());
            return false;
        }
    }

    // ── Row Mapper ────────────────────────────────────────────────────────────

    private ElephantSighting mapRow(ResultSet rs) throws SQLException {
        Location loc = new Location();
        loc.setId(rs.getInt("location_id"));
        loc.setName(rs.getString("loc_name"));
        loc.setDistrict(rs.getString("district"));
        try { loc.setLatitude(rs.getDouble("latitude")); }  catch (Exception ignored) {}
        try { loc.setLongitude(rs.getDouble("longitude")); } catch (Exception ignored) {}

        User reporter = new User();
        reporter.setId(rs.getInt("reported_by"));
        reporter.setFullName(rs.getString("reporter_name"));

        ElephantSighting s = new ElephantSighting();
        s.setId(rs.getInt("id"));
        s.setLocation(loc);
        s.setReportedBy(reporter);
        Timestamp sighted = rs.getTimestamp("sighted_at");
        if (sighted != null) s.setSightedAt(sighted.toLocalDateTime());
        s.setElephantCount(rs.getInt("elephant_count"));
        String hs = rs.getString("herd_size");
        if (hs != null) s.setHerdSize(ElephantSighting.HerdSize.valueOf(hs));
        String bh = rs.getString("behaviour");
        if (bh != null) s.setBehaviour(ElephantSighting.Behaviour.valueOf(bh));
        s.setNotes(rs.getString("notes"));
        s.setVerified(rs.getBoolean("verified"));
        return s;
    }

    private Connection conn() { return DatabaseManager.getInstance().getConnection(); }
}
