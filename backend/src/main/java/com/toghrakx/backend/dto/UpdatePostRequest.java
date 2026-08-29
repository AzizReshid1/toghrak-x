package com.toghrakx.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePostRequest {

    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Content cannot be empty")
    private String content;

    // Optional, can be null if category remains unchanged
    private Long categoryId;

    // Optional cover image URL
    private String coverImage;

    // Optional: list of image URLs to update/replace the existing images
    private List<String> imageUrls;
}
