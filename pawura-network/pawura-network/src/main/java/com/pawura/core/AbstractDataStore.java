package com.pawura.core;

import com.pawura.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * AbstractDataStore – ABSTRACTION for all data-access objects.
 * Provides common CRUD helpers; subclasses implement entity-specific SQL.
 *
 * @param <T>  The domain entity type (e.g., ElephantSighting)
 * @param <ID> The primary-key type (typically Integer)
 */
public abstract class AbstractDataStore<T, ID> {

    protected final Logger log = Logger.getLogger(getClass().getName());

    // ── Abstract CRUD ─────────────────────────────────────────────────────────
    public abstract boolean       save(T entity);
    public abstract boolean       update(T entity);
    public abstract boolean       delete(ID id);
    public abstract Optional<T>   findById(ID id);
    public abstract List<T>       findAll();

    // ── Common Helpers ────────────────────────────────────────────────────────

    /** Shortcut to obtain a live JDBC connection from the singleton. */
    protected Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    /**
     * Safely executes an INSERT/UPDATE/DELETE statement.
     * Returns true if at least one row was affected.
     */
    protected boolean executeUpdate(String sql, Object... params) {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            bindParams(ps, params);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.severe("SQL error in " + getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    /** Binds positional parameters to a PreparedStatement. */
    protected void bindParams(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    /** Returns the entity class name for logging purposes. */
    protected abstract String entityName();
}
