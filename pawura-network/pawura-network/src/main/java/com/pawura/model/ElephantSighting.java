package com.pawura.model;

import com.pawura.contract.Displayable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ElephantSighting – a confirmed sighting report.
 * Implements Displayable (POLYMORPHISM).
 */
public class ElephantSighting implements Displayable {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    public enum HerdSize { SOLITARY, SMALL_GROUP, MEDIUM_HERD, LARGE_HERD }
    public enum Behaviour { GRAZING, MOVING, AGGRESSIVE, BATHING, RESTING }

    private int           id;
    private Location      location;
    private User          reportedBy;
    private LocalDateTime sightedAt;
    private int           elephantCount;
    private HerdSize      herdSize;
    private Behaviour     behaviour;
    private String        notes;
    private String        caption;
    private String        imagePath;
    private boolean       isDanger;
    private boolean       isVisible;
    private boolean       verified;

    // ── Constructors ──────────────────────────────────────────────────────────
    public ElephantSighting() { 
        this.sightedAt = LocalDateTime.now(); 
        this.isVisible = true;
    }

    public ElephantSighting(Location location, User reportedBy,
                             int count, HerdSize herdSize, Behaviour behaviour) {
        this();
        this.location      = location;
        this.reportedBy    = reportedBy;
        this.elephantCount = count;
        this.herdSize      = herdSize;
        this.behaviour     = behaviour;
    }

    // ── Displayable (Polymorphism) ────────────────────────────────────────────
    @Override
    public String getDisplaySummary() {
        return String.format("🐘 %d elephant(s) @ %s  [%s]",
            elephantCount,
            location != null ? location.getName() : "Unknown",
            sightedAt.format(FMT));
    }

    @Override
    public String getDetailText() {
        return String.format("""
            Sighting ID   : %d
            Location      : %s (%s)
            Date / Time   : %s
            Elephant Count: %d
            Herd Size     : %s
            Caption       : %s
            Danger Alert  : %s
            Behaviour     : %s
            Reported By   : %s
            Verified      : %s
            Notes         : %s
            """,
            id,
            location != null ? location.getName()    : "—",
            location != null ? location.getDistrict() : "—",
            sightedAt.format(FMT),
            elephantCount,
            herdSize,
            caption != null ? caption : "—",
            isDanger ? "⚠ HIGH DANGER" : "Normal",
            behaviour,
            reportedBy != null ? reportedBy.getFullName() : "—",
            verified ? "✔ Yes" : "✘ Pending",
            notes != null ? notes : "—");
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int           getId()                         { return id; }
    public void          setId(int id)                   { this.id = id; }
    public Location      getLocation()                   { return location; }
    public void          setLocation(Location l)         { this.location = l; }
    public User          getReportedBy()                 { return reportedBy; }
    public void          setReportedBy(User u)           { this.reportedBy = u; }
    public LocalDateTime getSightedAt()                  { return sightedAt; }
    public void          setSightedAt(LocalDateTime t)   { this.sightedAt = t; }
    public int           getElephantCount()              { return elephantCount; }
    public void          setElephantCount(int n)         { this.elephantCount = Math.max(0, n); }
    public HerdSize      getHerdSize()                   { return herdSize; }
    public void          setHerdSize(HerdSize h)         { this.herdSize = h; }
    public Behaviour     getBehaviour()                  { return behaviour; }
    public void          setBehaviour(Behaviour b)       { this.behaviour = b; }
    public String        getNotes()                      { return notes; }
    public void          setNotes(String notes)          { this.notes = notes; }
    public String        getCaption()                    { return caption; }
    public void          setCaption(String caption)      { this.caption = caption; }
    public String        getImagePath()                  { return imagePath; }
    public void          setImagePath(String path)       { this.imagePath = path; }
    public boolean       isDanger()                      { return isDanger; }
    public void          setDanger(boolean danger)       { this.isDanger = danger; }
    public boolean       isVisible()                     { return isVisible; }
    public void          setVisible(boolean visible)     { this.isVisible = visible; }
    public boolean       isVerified()                    { return verified; }
    public void          setVerified(boolean verified)   { this.verified = verified; }
}
