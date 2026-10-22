package com.toghrakx.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.jsoup.Jsoup;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_category_id", columnList = "category_id"),
        @Index(name = "idx_posts_user_id", columnList = "user_id"),
        @Index(name = "idx_posts_view_count", columnList = "view_count"),
        @Index(name = "idx_posts_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    // columnDefinition = "TEXT" ensures PostgreSQL uses the TEXT type.
    // @Lob is intentionally omitted — it maps to CLOB in Hibernate 6, which
    // breaks JPQL functions like lower() and is unnecessary for PostgreSQL TEXT.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Plain-text copy of content (HTML tags stripped) used by search, so queries match
    // words readers see rather than markup like "span" or "href". Kept in sync on every save.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentText;

    // The main or featured image (optional)
    private String coverImage;

    // Which user created this post
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    // Which category the post belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private Instant createdAt;
    private Instant updatedAt;

    // One post can have many images
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PostImage> images = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
        contentText = Jsoup.parse(content).text();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
        contentText = Jsoup.parse(content).text();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o))
            return false;
        Post post = (Post) o;
        return id != null && id.equals(post.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
