package com.pawura.database;

import java.sql.*;
import java.util.logging.Logger;

/**
 * DatabaseManager – SINGLETON that owns the SQLite connection.
 * Creates all tables on first run and provides seeded demo data.
 */
public class DatabaseManager {

    private static final Logger  LOG      = Logger.getLogger(DatabaseManager.class.getName());
    private static final String  DB_URL   = "jdbc:sqlite:pawura.db";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void initialise() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            createTables();
            seedIfEmpty();
            LOG.info("Database initialised: " + DB_URL);
        } catch (SQLException e) {
            LOG.severe("Failed to initialise database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOG.info("Database connection closed.");
            }
        } catch (SQLException ignored) {}
    }

    public Connection getConnection() {
        return connection;
    }

    // ── DDL ───────────────────────────────────────────────────────────────────

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    UNIQUE NOT NULL,
                    password_hash TEXT    NOT NULL,
                    email         TEXT,
                    full_name     TEXT,
                    role          TEXT    NOT NULL DEFAULT 'VIEWER',
                    active        INTEGER NOT NULL DEFAULT 1,
                    created_at    TEXT    NOT NULL
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS locations (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT    NOT NULL,
                    district    TEXT,
                    latitude    REAL    NOT NULL,
                    longitude   REAL    NOT NULL,
                    description TEXT
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sightings (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    location_id     INTEGER REFERENCES locations(id),
                    reported_by     INTEGER REFERENCES users(id),
                    sighted_at      TEXT    NOT NULL,
                    elephant_count  INTEGER NOT NULL,
                    herd_size       TEXT,
                    behaviour       TEXT,
                    notes           TEXT,
                    verified        INTEGER NOT NULL DEFAULT 0
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS predictions (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    predicted_location  INTEGER REFERENCES locations(id),
                    predicted_at        TEXT    NOT NULL,
                    expected_arrival    TEXT,
                    confidence_score    REAL,
                    algorithm           TEXT,
                    notes               TEXT
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS news_articles (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    title        TEXT    NOT NULL,
                    content      TEXT,
                    author       TEXT,
                    source       TEXT,
                    category     TEXT,
                    published_at TEXT    NOT NULL,
                    image_url    TEXT
                )""");
        }
    }

    // ── Seed Data ─────────────────────────────────────────────────────────────

    private void seedIfEmpty() throws SQLException {
        try (ResultSet rs = connection.createStatement()
                .executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.getInt(1) > 0) return;
        }

        // Passwords are BCrypt hashes of "password123"
        String hash = "$2a$10$Nf5oXvjV7R.R9NEoZU8RXO7OqVCB2e7bF0n3vLnBUMTlf/WiC5Ehu";

        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO users(username,password_hash,email,full_name,role,created_at) VALUES(?,?,?,?,?,?)")) {

            Object[][] users = {
                {"admin",  hash, "admin@pawura.lk",  "Kasun Perera",    "ADMINISTRATOR", "2024-01-01T00:00"},
                {"ranger1",hash, "r1@pawura.lk",     "Amaya Silva",     "RANGER",        "2024-01-02T00:00"},
                {"viewer1",hash, "v1@pawura.lk",     "Nuwan Fernando",  "VIEWER",        "2024-01-03T00:00"},
            };
            for (Object[] u : users) {
                for (int i = 0; i < u.length; i++) ps.setObject(i + 1, u[i]);
                ps.executeUpdate();
            }
        }

        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO locations(name,district,latitude,longitude) VALUES(?,?,?,?)")) {

            Object[][] locs = {
                {"Minneriya Tank",      "Polonnaruwa",  8.0312, 80.9010},
                {"Kaudulla NP",         "Trincomalee",  8.1667, 80.9167},
                {"Udawalawe NP",        "Monaragala",   6.4754, 80.8868},
                {"Wasgamuwa NP",        "Matale",       7.7236, 80.8656},
                {"Yala NP",             "Hambantota",   6.3729, 81.5218},
                {"Lunugamvehera NP",    "Hambantota",   6.2947, 81.2669},
            };
            for (Object[] l : locs) {
                for (int i = 0; i < l.length; i++) ps.setObject(i + 1, l[i]);
                ps.executeUpdate();
            }
        }

        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO sightings(location_id,reported_by,sighted_at,elephant_count,herd_size,behaviour,notes,verified) VALUES(?,?,?,?,?,?,?,?)")) {

            Object[][] sightings = {
                {1, 2, "2024-08-15T14:30", 47, "LARGE_HERD", "GRAZING",    "Annual Minneriya gathering",  1},
                {2, 2, "2024-09-01T08:15", 12, "MEDIUM_HERD","MOVING",     "Heading towards tank",         0},
                {3, 1, "2024-10-20T17:00",  5, "SMALL_GROUP","BATHING",    "Calves present",               1},
                {5, 2, "2024-11-05T06:45",  1, "SOLITARY",   "AGGRESSIVE", "Bull in musth – keep away",    1},
            };
            for (Object[] s : sightings) {
                for (int i = 0; i < s.length; i++) ps.setObject(i + 1, s[i]);
                ps.executeUpdate();
            }
        }

        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO news_articles(title,content,author,source,category,published_at) VALUES(?,?,?,?,?,?)")) {

            Object[][] articles = {
                {"Minneriya Elephant Gathering Sets Record",
                 "Over 300 elephants were recorded at the annual Minneriya gathering this year, the highest count in a decade.",
                 "Priya Jayawardena", "Daily Mirror LK", "SIGHTING", "2024-08-20T00:00"},
                {"Electric Fences Reduce Human-Elephant Conflict in Ampara",
                 "A new government initiative installing solar-powered electric fences has reduced crop raiding incidents by 60% in the Ampara district.",
                 "Ravi Wickramasinghe", "The Island", "POLICY", "2024-09-15T00:00"},
                {"AI Tracking System Predicts Elephant Movements",
                 "Researchers at the University of Peradeniya have developed a machine-learning model to predict elephant movement corridors with 78% accuracy.",
                 "Dr. Nimal Abeyrathna", "Sunday Observer", "RESEARCH", "2024-10-02T00:00"},
            };
            for (Object[] a : articles) {
                for (int i = 0; i < a.length; i++) ps.setObject(i + 1, a[i]);
                ps.executeUpdate();
            }
        }

        LOG.info("Database seeded with demo data.");
    }
}
