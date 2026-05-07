package com.pawura;

import java.time.LocalDateTime;

/**
 * Sighting – Represents an elephant sighting report.
 */
public class Sighting {
    private int id;
    private int locationId;
    private int reportedBy;
    private LocalDateTime sightedAt;
    private int elephantCount;
    private String herdSize;
    private String behaviour;
    private String notes;
    private boolean verified;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getLocationId() { return locationId; }
    public void setLocationId(int locationId) { this.locationId = locationId; }

    public int getReportedBy() { return reportedBy; }
    public void setReportedBy(int reportedBy) { this.reportedBy = reportedBy; }

    public LocalDateTime getSightedAt() { return sightedAt; }
    public void setSightedAt(LocalDateTime sightedAt) { this.sightedAt = sightedAt; }

    public int getElephantCount() { return elephantCount; }
    public void setElephantCount(int count) { this.elephantCount = count; }

    public String getHerdSize() { return herdSize; }
    public void setHerdSize(String size) { this.herdSize = size; }

    public String getBehaviour() { return behaviour; }
    public void setBehaviour(String behaviour) { this.behaviour = behaviour; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    @Override
    public String toString() {
        return "Sighting #" + id + " [" + elephantCount + " elephants]";
    }
}