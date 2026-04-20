package com.pawura.model;

/**
 * Location – ENCAPSULATION of a geographic point in Sri Lanka.
 * Validates latitude/longitude ranges on assignment.
 */
public class Location {

    private int    id;
    private double latitude;
    private double longitude;
    private String name;         // Human-readable place name
    private String district;     // e.g. "Trincomalee", "Ampara"
    private String description;

    // ── Constructors ──────────────────────────────────────────────────────────
    public Location() {}

    public Location(double latitude, double longitude, String name, String district) {
        setLatitude(latitude);
        setLongitude(longitude);
        this.name     = name;
        this.district = district;
    }

    // ── Business Methods ──────────────────────────────────────────────────────

    /**
     * Returns the straight-line distance (km) to another location
     * using the Haversine formula.
     */
    public double distanceTo(Location other) {
        final double R = 6371.0; // Earth radius km
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(this.latitude))
                   * Math.cos(Math.toRadians(other.latitude))
                   * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int    getId()                { return id; }
    public void   setId(int id)          { this.id = id; }

    public double getLatitude()          { return latitude; }
    public void   setLatitude(double lat) {
        if (lat < -90 || lat > 90)
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        this.latitude = lat;
    }

    public double getLongitude()         { return longitude; }
    public void   setLongitude(double lon) {
        if (lon < -180 || lon > 180)
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        this.longitude = lon;
    }

    public String getName()              { return name; }
    public void   setName(String name)   { this.name = name; }

    public String getDistrict()          { return district; }
    public void   setDistrict(String d)  { this.district = d; }

    public String getDescription()       { return description; }
    public void   setDescription(String d){ this.description = d; }

    @Override
    public String toString() {
        return String.format("%s (%s) [%.4f, %.4f]", name, district, latitude, longitude);
    }
}
