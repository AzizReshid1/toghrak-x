package com.toghrakx.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PostResponse {
    private Long id;
    private String title;
    private String slug;
    private String content;
    private String coverImage;
    private Long categoryId;
    private String categoryName;
    private String authorUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long viewCount;
    private List<String> imageUrls;
    /** Plain-text excerpt (HTML stripped, max 200 chars). Use this in list/card views. */
    private String excerpt;
    /** Tells the client how to render {@code content}. Always "html" for now. */
    private String contentType;
}
