package com.pawura.database;

import java.sql.*;
import java.util.logging.Logger;

/**
 * DatabaseManager – SINGLETON that owns the SQLite connection.
 * Creates all tables on first run and provides seeded demo data.
 */
public class DatabaseManager {

    private static final Logger  LOG      = Logger.getLogger(DatabaseManager.class.getName());
    
    // Base URL to connect to the server itself
    private static final String  SERVER_URL = "jdbc:mysql://127.0.0.1:3306/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
    
    // MySQL Connection URL - REPLACE WITH YOUR ACTUAL DATABASE DETAILS
    private static final String  DB_URL   = "jdbc:mysql://127.0.0.1:3306/pawura_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
    private static final String  DB_USER  = "root"; // Update this with your MySQL username
    private static final String  DB_PASS  = "";     // Update this with your MySQL password

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
            // 1. Connect to the server to ensure the database exists
            try (Connection serverConn = DriverManager.getConnection(SERVER_URL, DB_USER, DB_PASS);
                 Statement st = serverConn.createStatement()) {
                // Ensure the database exists without dropping it
                st.executeUpdate("CREATE DATABASE IF NOT EXISTS pawura_db");
            }

            // 2. Now connect to the specific database
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            createTables();
            seedIfEmpty();
            LOG.info("Database initialised: " + DB_URL);
        } catch (SQLException e) {
            LOG.severe("COULD NOT CONNECT TO MYSQL. Ensure MySQL is running and credentials in DatabaseManager.java are correct.");
            LOG.severe("Error Detail: " + e.getMessage());
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
                    id            INT AUTO_INCREMENT PRIMARY KEY,
                    username      VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    email         VARCHAR(255) UNIQUE NOT NULL,
                    full_name     VARCHAR(255),
                    role          VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
                    active        BOOLEAN NOT NULL DEFAULT TRUE,
                    is_verified   BOOLEAN NOT NULL DEFAULT FALSE,
                    otp_code      VARCHAR(10),
                    otp_expiry    DATETIME,
                    created_at    DATETIME NOT NULL
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS locations (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    name        VARCHAR(255) NOT NULL,
                    district    VARCHAR(255),
                    latitude    DOUBLE NOT NULL,
                    longitude   DOUBLE NOT NULL,
                    description TEXT
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sightings (
                    id              INT AUTO_INCREMENT PRIMARY KEY,
                    location_id     INT REFERENCES locations(id),
                    reported_by     INT REFERENCES users(id),
                    sighted_at      DATETIME NOT NULL,
                    elephant_count  INT NOT NULL,
                    herd_size       VARCHAR(50),
                    behaviour       VARCHAR(50),
                    notes           TEXT,
                    verified        BOOLEAN NOT NULL DEFAULT FALSE
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS predictions (
                    id                  INT AUTO_INCREMENT PRIMARY KEY,
                    predicted_location  INT REFERENCES locations(id),
                    predicted_at        DATETIME NOT NULL,
                    expected_arrival    DATETIME,
                    confidence_score    DOUBLE,
                    algorithm           VARCHAR(255),
                    notes               TEXT
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS news_articles (
                    id           INT AUTO_INCREMENT PRIMARY KEY,
                    title        VARCHAR(255) NOT NULL,
                    content      TEXT,
                    author       VARCHAR(255),
                    source       VARCHAR(255),
                    category     VARCHAR(255),
                    published_at DATETIME NOT NULL,
                    image_url    TEXT
                )""");
        }
    }

    // ── Seed Data ─────────────────────────────────────────────────────────────

    private void seedIfEmpty() throws SQLException {
        try (ResultSet rs = connection.createStatement()
                .executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }

        // Passwords are BCrypt hashes of "password123"
        String hash = "$2a$10$Nf5oXvjV7R.R9NEoZU8RXO7OqVCB2e7bF0n3vLnBUMTlf/WiC5Ehu"; // BCrypt hash of "password123"

        try (PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO users(username,password_hash,email,full_name,role,active,is_verified,otp_code,otp_expiry,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)")) {

            Object[][] users = {
                {"admin",  hash, "admin@pawura.lk",  "Kasun Perera",    "ADMINISTRATOR", true, true, null, null, "2024-01-01 00:00:00"},
                {"ranger1",hash, "r1@pawura.lk",     "Amaya Silva",     "RANGER",        true, true, null, null, "2024-01-02 00:00:00"},
                {"viewer1",hash, "v1@pawura.lk",     "Nuwan Fernando",  "VIEWER",        true, true, null, null, "2024-01-03 00:00:00"},
                {"demo",   hash, "demo@pawura.lk",    "Demo User",       "VIEWER",        true, true, null, null, "2024-11-20 00:00:00"},
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
                {1, 2, "2024-08-15 14:30:00", 47, "LARGE_HERD", "GRAZING",    "Annual Minneriya gathering",  1},
                {2, 2, "2024-09-01 08:15:00", 12, "MEDIUM_HERD","MOVING",     "Heading towards tank",         0},
                {3, 1, "2024-10-20 17:00:00",  5, "SMALL_GROUP","BATHING",    "Calves present",               1},
                {5, 2, "2024-11-05 06:45:00",  1, "SOLITARY",   "AGGRESSIVE", "Bull in musth – keep away",    1},
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
                 "Priya Jayawardena", "Daily Mirror LK", "SIGHTING", "2024-08-20 00:00:00"},
                {"Electric Fences Reduce Human-Elephant Conflict in Ampara",
                 "A new government initiative installing solar-powered electric fences has reduced crop raiding incidents by 60% in the Ampara district.",
                 "Ravi Wickramasinghe", "The Island", "POLICY", "2024-09-15 00:00:00"},
                {"AI Tracking System Predicts Elephant Movements",
                 "Researchers at the University of Peradeniya have developed a machine-learning model to predict elephant movement corridors with 78% accuracy.",
                 "Dr. Nimal Abeyrathna", "Sunday Observer", "RESEARCH", "2024-10-02 00:00:00"},
            };
            for (Object[] a : articles) {
                for (int i = 0; i < a.length; i++) ps.setObject(i + 1, a[i]);
                ps.executeUpdate();
            }
        }

        LOG.info("Database seeded with demo data.");
    }
}
