package com.pawura.service;

import com.pawura.core.AbstractPredictionModel;
import com.pawura.database.DatabaseManager;
import com.pawura.model.Location;
import com.pawura.model.Prediction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * PredictionService – extends AbstractPredictionModel (ABSTRACTION / POLYMORPHISM).
 * Implements a simple linear-extrapolation movement predictor.
 */
public class PredictionService extends AbstractPredictionModel {

    private static final Logger LOG = Logger.getLogger(PredictionService.class.getName());

    public PredictionService() {
        super("LinearExtrapolation", "1.0");
    }

    // ── AbstractPredictionModel implementation ────────────────────────────────

    @Override
    protected Prediction compute(List<Location> history) {
        if (history.size() < 2) return buildFallbackPrediction("Need ≥2 locations.");

        // Average movement vector
        Location first = history.get(0);
        Location last  = history.get(history.size() - 1);
        int      steps = history.size() - 1;

        double deltaLat = (last.getLatitude()  - first.getLatitude())  / steps;
        double deltaLon = (last.getLongitude() - first.getLongitude()) / steps;

        Location predicted = new Location(
            last.getLatitude()  + deltaLat,
            last.getLongitude() + deltaLon,
            "Predicted Zone",
            last.getDistrict()
        );

        double distanceTravelled = first.distanceTo(last);
        double confidence = Math.max(0.1, 1.0 - (distanceTravelled / 200.0));

        Prediction p = new Prediction();
        p.setPredictedLocation(predicted);
        p.setConfidenceScore(Math.min(confidence, 0.9));
        p.setExpectedArrival(LocalDateTime.now().plusHours(6));
        p.setNotes(String.format(
            "Extrapolated from %d waypoints. Avg step: %.2f km.", steps, distanceTravelled / steps));
        return p;
    }

    @Override
    protected int getMinimumHistorySize() { return 2; }

    // ── Persistence ───────────────────────────────────────────────────────────

    public boolean save(Prediction p) {
        String sql = """
            INSERT INTO predictions(predicted_location,predicted_at,expected_arrival,
                                    confidence_score,algorithm,notes)
            VALUES(?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, p.getPredictedLocation() != null
                            ? p.getPredictedLocation().getId() : null);
            ps.setString(2, p.getPredictedAt().toString());
            ps.setString(3, p.getExpectedArrival() != null
                            ? p.getExpectedArrival().toString() : null);
            ps.setDouble(4, p.getConfidenceScore());
            ps.setString(5, p.getAlgorithm());
            ps.setString(6, p.getNotes());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) p.setId(k.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            LOG.severe("Save prediction error: " + e.getMessage());
            return false;
        }
    }

    public List<Prediction> findAll() {
        List<Prediction> list = new ArrayList<>();
        String sql = """
            SELECT p.*, l.name loc_name, l.district, l.latitude, l.longitude
            FROM predictions p
            LEFT JOIN locations l ON l.id = p.predicted_location
            ORDER BY p.predicted_at DESC""";
        try (ResultSet rs = conn().createStatement().executeQuery(sql)) {
            while (rs.next()) {
                Prediction p = new Prediction();
                p.setId(rs.getInt("id"));
                Location loc = new Location();
                loc.setName(rs.getString("loc_name"));
                loc.setDistrict(rs.getString("district"));
                try { loc.setLatitude(rs.getDouble("latitude")); }  catch (Exception ignored) {}
                try { loc.setLongitude(rs.getDouble("longitude")); } catch (Exception ignored) {}
                p.setPredictedLocation(loc);
                String pa = rs.getString("predicted_at");
                if (pa != null) p.setPredictedAt(LocalDateTime.parse(pa));
                String ea = rs.getString("expected_arrival");
                if (ea != null) p.setExpectedArrival(LocalDateTime.parse(ea));
                p.setConfidenceScore(rs.getDouble("confidence_score"));
                p.setAlgorithm(rs.getString("algorithm"));
                p.setNotes(rs.getString("notes"));
                list.add(p);
            }
        } catch (SQLException e) {
            LOG.severe("FindAll predictions error: " + e.getMessage());
        }
        return list;
    }

    private Connection conn() { return DatabaseManager.getInstance().getConnection(); }
}
