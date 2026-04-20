package com.pawura.service;

import com.pawura.database.DatabaseManager;
import com.pawura.model.NewsArticle;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * NewsService – persistence and retrieval of NewsArticle objects.
 */
public class NewsService {

    private static final Logger LOG = Logger.getLogger(NewsService.class.getName());

    // ── Save ──────────────────────────────────────────────────────────────────

    public boolean save(NewsArticle a) {
        String sql = """
            INSERT INTO news_articles(title,content,author,source,category,published_at,image_url)
            VALUES(?,?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getContent());
            ps.setString(3, a.getAuthor());
            ps.setString(4, a.getSource());
            ps.setString(5, a.getCategory() != null ? a.getCategory().name() : null);
            ps.setString(6, a.getPublishedAt().toString());
            ps.setString(7, a.getImageUrl());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) a.setId(k.getInt(1));
            }
            LOG.info("Saved article: " + a.getTitle());
            return true;
        } catch (SQLException e) {
            LOG.severe("Save article error: " + e.getMessage());
            return false;
        }
    }

    // ── Fetch All ─────────────────────────────────────────────────────────────

    public List<NewsArticle> findAll() {
        List<NewsArticle> list = new ArrayList<>();
        String sql = "SELECT * FROM news_articles ORDER BY published_at DESC";
        try (ResultSet rs = conn().createStatement().executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOG.severe("FindAll articles error: " + e.getMessage());
        }
        return list;
    }

    // ── Fetch by Category ─────────────────────────────────────────────────────

    public List<NewsArticle> findByCategory(NewsArticle.Category category) {
        List<NewsArticle> list = new ArrayList<>();
        String sql = "SELECT * FROM news_articles WHERE category=? ORDER BY published_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, category.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOG.severe("FindByCategory error: " + e.getMessage());
        }
        return list;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public boolean delete(int id) {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM news_articles WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.severe("Delete article error: " + e.getMessage());
            return false;
        }
    }

    // ── Row Mapper ────────────────────────────────────────────────────────────

    private NewsArticle mapRow(ResultSet rs) throws SQLException {
        NewsArticle a = new NewsArticle();
        a.setId(rs.getInt("id"));
        a.setTitle(rs.getString("title"));
        a.setContent(rs.getString("content"));
        a.setAuthor(rs.getString("author"));
        a.setSource(rs.getString("source"));
        String cat = rs.getString("category");
        if (cat != null) {
            try { a.setCategory(NewsArticle.Category.valueOf(cat)); }
            catch (IllegalArgumentException ignored) {}
        }
        String pub = rs.getString("published_at");
        if (pub != null) a.setPublishedAt(LocalDateTime.parse(pub));
        a.setImageUrl(rs.getString("image_url"));
        return a;
    }

    private Connection conn() { return DatabaseManager.getInstance().getConnection(); }
}
