package com.pawura.model;

import com.pawura.contract.Displayable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * NewsArticle – a news item about elephant-human coexistence.
 * Implements Displayable (POLYMORPHISM).
 */
public class NewsArticle implements Displayable {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy");

    public enum Category { CONFLICT, CONSERVATION, SIGHTING, POLICY, RESEARCH }

    private int           id;
    private String        title;
    private String        content;
    private String        author;
    private String        source;
    private Category      category;
    private LocalDateTime publishedAt;
    private String        imageUrl;

    // ── Constructors ──────────────────────────────────────────────────────────
    public NewsArticle() { this.publishedAt = LocalDateTime.now(); }

    public NewsArticle(String title, String content, String author,
                       Category category, String source) {
        this();
        this.title    = title;
        this.content  = content;
        this.author   = author;
        this.category = category;
        this.source   = source;
    }

    // ── Displayable (Polymorphism) ────────────────────────────────────────────
    @Override
    public String getDisplaySummary() {
        return String.format("[%s]  %s  –  %s",
            category, title, publishedAt.format(FMT));
    }

    @Override
    public String getDetailText() {
        return String.format("""
            %s
            %s
            
            Category : %s
            Author   : %s
            Source   : %s
            Published: %s
            
            %s
            """,
            title,
            "─".repeat(Math.min(title.length(), 60)),
            category,
            author != null ? author : "—",
            source  != null ? source  : "—",
            publishedAt.format(FMT),
            content);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int           getId()                        { return id; }
    public void          setId(int id)                  { this.id = id; }
    public String        getTitle()                     { return title; }
    public void          setTitle(String t)             { this.title = t; }
    public String        getContent()                   { return content; }
    public void          setContent(String c)           { this.content = c; }
    public String        getAuthor()                    { return author; }
    public void          setAuthor(String a)            { this.author = a; }
    public String        getSource()                    { return source; }
    public void          setSource(String s)            { this.source = s; }
    public Category      getCategory()                  { return category; }
    public void          setCategory(Category c)        { this.category = c; }
    public LocalDateTime getPublishedAt()               { return publishedAt; }
    public void          setPublishedAt(LocalDateTime t){ this.publishedAt = t; }
    public String        getImageUrl()                  { return imageUrl; }
    public void          setImageUrl(String url)        { this.imageUrl = url; }
}
