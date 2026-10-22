package com.toghrakx.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A post as shown in lists and cards: everything except the full HTML content and gallery.
 * Keeps list responses small; the full post comes from PostResponse on the detail page.
 */
@Getter
@Setter
public class PostSummary {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String coverImage;
    private Long categoryId;
    private String categoryName;
    private String authorUsername;
    private Instant createdAt;
    private Instant updatedAt;
    private Long viewCount;
}
