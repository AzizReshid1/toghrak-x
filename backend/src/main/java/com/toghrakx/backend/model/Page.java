package com.toghrakx.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.jsoup.Jsoup;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pages", indexes = {
        @Index(name = "idx_pages_updated_at", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    // columnDefinition = "TEXT" ensures PostgreSQL uses the TEXT type.
    // @Lob is intentionally omitted — see Post.java for the full explanation.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Plain-text copy of content used by search — see Post.contentText.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentText;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "page_images", joinColumns = @JoinColumn(name = "page_id"), indexes = {
            @Index(name = "idx_page_images_page_id", columnList = "page_id")
    })
    @Column(name = "image_url")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        updatedAt = LocalDateTime.now();
        contentText = Jsoup.parse(content).text();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
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
        Page page = (Page) o;
        return id != null && id.equals(page.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
